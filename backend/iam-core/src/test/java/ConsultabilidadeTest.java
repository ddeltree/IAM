import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import poo.iam.Action;
import poo.iam.Effect;
import poo.iam.Group;
import poo.iam.Iam;
import poo.iam.IamFactory;
import poo.iam.MembershipManager;
import poo.iam.Permission;
import poo.iam.Resource;
import poo.iam.ResourceType;
import poo.iam.Statement;
import poo.iam.User;
import poo.iam.condition.Condition;
import poo.iam.query.PredicateRenderer;
import poo.iam.query.SqlWhereRenderer;
import poo.iam.spi.AttributeProvider;
import poo.iam.spi.PrincipalDirectory;
import poo.iam.spi.SqlMapping;

/**
 * O contrato que nenhuma capacidade nova pode quebrar.
 *
 * A regra é uma só: <b>a extração de restrição só escolhe candidatos; o motor
 * avalia cada sobrevivente</b>. Um erro no extrator custa desempenho, nunca
 * acesso indevido. Ela vale porque estes testes a conferem sobre a matriz
 * inteira — e é justamente por isso que curinga, padrão de recurso e
 * operadores de ordem precisaram entrar acompanhados de poda correspondente,
 * em vez de depois.
 */
class ConsultabilidadeTest {

  enum Acao implements Action {
    LER, APAGAR
  }

  enum Tipo implements ResourceType {
    ARQUIVO
  }

  record Arquivo(String id, String autorId, String tamanho) implements Resource {
    public ResourceType getType() {
      return Tipo.ARQUIVO;
    }

    public String getId() {
      return id;
    }
  }

  static final Permission LER = new Permission(Acao.LER, Tipo.ARQUIVO);
  static final Permission APAGAR = new Permission(Acao.APAGAR, Tipo.ARQUIVO);

  private Iam iam;
  private List<User> usuarios;
  private List<Arquivo> arquivos;

  @BeforeEach
  void montar() {
    var provedor = new AttributeProvider() {
      public ResourceType tipo() {
        return Tipo.ARQUIVO;
      }

      public Map<String, List<String>> atributosDe(Resource r) {
        var a = (Arquivo) r;
        return Map.of("autorId", List.of(a.autorId()), "tamanho", List.of(a.tamanho()));
      }
    };

    var ana = new User("1", "Ana");
    var bruno = new User("2", "Bruno");
    var carla = new User("3", "Carla");
    usuarios = List.of(ana, bruno, carla);

    var todos = new Group("Todos");
    var moderadores = new Group("Moderadores");
    for (User u : usuarios)
      MembershipManager.link(u, todos);
    MembershipManager.link(carla, moderadores);

    // uma de cada coisa que entrou: condição com variável, padrão de recurso
    // mirando uma instância, curinga na ação e comparação de ordem
    todos.grantPermission(LER, Condition.igual("recurso:autorId", "${principal:id}"));
    todos.add(Statement.de(Effect.ALLOW, "LER", "ARQUIVO/a9", Condition.SEMPRE));
    moderadores.add(Statement.allow("*", "*"));
    moderadores.add(Statement.de(Effect.DENY, "APAGAR", "*",
        Condition.maiorQue("recurso:tamanho", "100")));

    iam = IamFactory.novo()
        .atributos(provedor)
        .principais(new PrincipalDirectory() {
          public Collection<User> usuarios() {
            return usuarios;
          }

          public Collection<Group> grupos() {
            return List.of(todos, moderadores);
          }
        })
        .construir();

    arquivos = List.of(
        new Arquivo("a1", "1", "10"),
        new Arquivo("a2", "2", "500"),
        new Arquivo("a9", "3", "50"),
        new Arquivo("a4", "3", "999"));
  }

  @Test
  void aPodaEAVarreduraConcordamSempre() {
    var comparacoes = 0;
    for (Permission permissao : List.of(LER, APAGAR)) {
      for (Arquivo arquivo : arquivos) {
        // conjunto, não lista: a poda visita os grupos antes dos principais
        // inline, então a ordem difere da varredura — o que a regra promete é
        // que ninguém entra nem sai
        var podado = Set.copyOf(iam.consultas().quemPode(permissao, arquivo).principais);
        var varrido = Set.copyOf(iam.consultas().quemPodeVarrendo(permissao, arquivo));
        assertEquals(varrido, podado,
            "a poda mudou a resposta em " + permissao + " sobre " + arquivo.id());
        comparacoes++;
      }
    }
    assertTrue(comparacoes >= 8, "poucas comparações: " + comparacoes);
  }

  @Test
  void aPodaSoEncolheNuncaDecide() {
    // o teste acima confere a igualdade do resultado; este confere a razão de
    // ela valer: quem decide é o motor, sempre, sobre cada candidato
    for (Permission permissao : List.of(LER, APAGAR)) {
      for (Arquivo arquivo : arquivos) {
        var resultado = iam.consultas().quemPode(permissao, arquivo);
        assertTrue(resultado.avaliados <= resultado.conhecidos,
            "a poda considerou mais gente do que existe");
        for (User permitido : resultado.principais)
          assertTrue(iam.motor().isAllowed(permitido, permissao, arquivo),
              permitido.getName() + " passou pela poda mas o motor nega");
      }
    }
  }

  @Test
  void oFiltroConcordaComOMotorArquivoAArquivo() {
    for (User quem : usuarios) {
      for (Permission permissao : List.of(LER, APAGAR)) {
        var pelaConsulta = iam.consultas().filtrar(quem, permissao, arquivos);
        var peloMotor = arquivos.stream()
            .filter(a -> iam.motor().isAllowed(quem, permissao, a))
            .toList();
        assertEquals(peloMotor, pelaConsulta,
            "o filtro divergiu do motor para " + quem.getName() + " em " + permissao);
      }
    }
  }

  @Test
  void oPadraoDeRecursoEntraNoFiltro() {
    // Bruno é autor do a2; e o a9 é alcançado pela cláusula que mira a instância
    var bruno = usuarios.get(1);
    var filtro = iam.consultas().ondePosso(bruno, LER);
    var predicado = PredicateRenderer.render(filtro, iam.contexto());

    assertTrue(predicado.test(arquivos.get(1)), "a2 é dele");
    assertTrue(predicado.test(arquivos.get(2)), "a9 está mirado pelo padrão de recurso");
    assertFalse(predicado.test(arquivos.get(3)), "a4 não é dele nem está mirado");

    // e o mesmo filtro, escrito em SQL: uma restrição, dois destinos
    var sql = SqlWhereRenderer.render(filtro, new SqlMapping() {
      public String igual(String chave, String valor) {
        return switch (chave) {
          case "recurso:autorId" -> "arquivo.autor_id = '" + valor + "'";
          case "recurso:id" -> "arquivo.id = '" + valor + "'";
          default -> null;
        };
      }

      public String contem(String chave, String valor) {
        return null;
      }
    });
    assertTrue(sql.contains("arquivo.autor_id = '2'"), sql);
    assertTrue(sql.contains("arquivo.id = 'a9'"), sql);
  }

  @Test
  void aComparacaoDeOrdemViraFiltroEViraSql() {
    var filtro = new poo.iam.query.ResourceConstraint.AtributoCompara(
        "recurso:tamanho", ">", "100");

    var predicado = PredicateRenderer.render(filtro, iam.contexto());
    assertFalse(predicado.test(arquivos.get(0)), "10 não é maior que 100");
    assertTrue(predicado.test(arquivos.get(1)), "500 é");
    // e comparando como número, não como texto: "999" > "100" nos dois sentidos,
    // mas "50" > "100" só seria verdade em ordem alfabética
    assertFalse(predicado.test(arquivos.get(2)), "50 não é maior que 100");

    var sql = SqlWhereRenderer.render(filtro, new SqlMapping() {
      public String igual(String chave, String valor) {
        return null;
      }

      public String contem(String chave, String valor) {
        return null;
      }

      @Override
      public String compara(String chave, String operador, String valor) {
        return "arquivo.tamanho " + operador + " " + valor;
      }
    });
    assertEquals("arquivo.tamanho > 100", sql);
  }

  @Test
  void semTraducaoOFiltroNaoFiltraEmVezDeExcluir() {
    // uma chave que o mapeamento não conhece precisa virar "1=1", nunca "1=0":
    // filtro que exclui por não entender esconderia acesso legítimo
    var filtro = new poo.iam.query.ResourceConstraint.AtributoIgual("chave:desconhecida", "x");
    var sql = SqlWhereRenderer.render(filtro, new SqlMapping() {
      public String igual(String chave, String valor) {
        return null;
      }

      public String contem(String chave, String valor) {
        return null;
      }
    });
    assertEquals("1=1", sql);
  }

  @Test
  void oCurigaNaAcaoNaoEscapaDasConsultas() {
    // Carla é moderadora: recebe tudo por allow("*", "*"), e mesmo assim
    // aparece nas respostas de quem-pode — que é o que o catálogo garante
    var carla = usuarios.get(2);
    var pequeno = arquivos.get(0);

    assertTrue(iam.motor().isAllowed(carla, APAGAR, pequeno));
    assertTrue(iam.consultas().quemPode(APAGAR, pequeno).principais.contains(carla),
        "concedido por curinga e invisível à consulta seria trocar consultabilidade "
            + "por expressividade");

    // e o DENY condicional a exclui do arquivo grande, sem excluí-la do pequeno
    var grande = arquivos.get(1);
    assertFalse(iam.consultas().quemPode(APAGAR, grande).principais.contains(carla));
  }
}
