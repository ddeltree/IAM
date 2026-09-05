import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Collection;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import poo.iam.Action;
import poo.iam.Group;
import poo.iam.IamFactory;
import poo.iam.MembershipManager;
import poo.iam.Permission;
import poo.iam.Resource;
import poo.iam.ResourceType;
import poo.iam.User;
import poo.iam.document.PolicyRepository;
import poo.iam.query.SqlWhereRenderer;
import poo.iam.spi.ActionCatalog;
import poo.iam.spi.AttributeProvider;
import poo.iam.spi.PrincipalDirectory;
import poo.iam.spi.SqlMapping;

/**
 * Um sistema inteiro adaptado ao núcleo, do zero, em um arquivo.
 *
 * Este teste é a resposta executável para "o que eu preciso escrever para usar
 * isto?". Não há classroom aqui, e não há nada do domínio dentro do núcleo: um
 * repositório de arquivos, quatro implementações do {@code poo.iam.spi}, uma
 * política em documento, e as cinco consultas respondendo.
 *
 * O README do módulo é este exemplo em prosa. Ele é teste, e não só texto, para
 * que envelhecer deixe de ser uma opção.
 */
class ExemploDeAdaptacaoTest {

  // ─────────── 1. o vocabulário do domínio ───────────

  enum Acao implements Action {
    LER, ESCREVER, APAGAR
  }

  enum Tipo implements ResourceType {
    ARQUIVO
  }

  record Arquivo(String id, String donoId, String bytes) implements Resource {
    public ResourceType getType() {
      return Tipo.ARQUIVO;
    }

    public String getId() {
      return id;
    }
  }

  static final Permission LER = new Permission(Acao.LER, Tipo.ARQUIVO);
  static final Permission ESCREVER = new Permission(Acao.ESCREVER, Tipo.ARQUIVO);
  static final Permission APAGAR = new Permission(Acao.APAGAR, Tipo.ARQUIVO);
  static final List<Permission> TUDO = List.of(LER, ESCREVER, APAGAR);

  // ─────────── 2. como o núcleo lê um arquivo ───────────

  static final AttributeProvider ATRIBUTOS = new AttributeProvider() {
    public ResourceType tipo() {
      return Tipo.ARQUIVO;
    }

    public Map<String, List<String>> atributosDe(Resource r) {
      var a = (Arquivo) r;
      return Map.of("donoId", List.of(a.donoId()), "bytes", List.of(a.bytes()));
    }
  };

  // ─────────── 3. como a mesma chave se escreve no banco ───────────

  static final SqlMapping SQL = new SqlMapping() {
    public String igual(String chave, String valor) {
      return chave.equals("recurso:donoId") ? "arquivo.dono_id = '" + valor + "'" : null;
    }

    public String contem(String chave, String valor) {
      return null;
    }

    @Override
    public String compara(String chave, String operador, String valor) {
      return chave.equals("recurso:bytes") ? "arquivo.bytes " + operador + " " + valor : null;
    }
  };

  // ─────────── 4. a política, como documento ───────────

  static final Map<String, Object> DOCUMENTO = Map.of("politicas", List.of(
      Map.of("nome", "Colaborador", "statements", List.of(
          Map.of("effect", "ALLOW", "action", "*", "resource", "ARQUIVO",
              "condition", Map.of("Igual", Map.of("recurso:donoId", List.of("${principal:id}")))),
          Map.of("effect", "ALLOW", "action", "LER", "resource", "ARQUIVO"))),
      Map.of("nome", "Faxina", "statements", List.of(
          Map.of("effect", "ALLOW", "action", "APAGAR", "resource", "ARQUIVO",
              "condition", Map.of("Maior", Map.of("recurso:bytes", List.of("1000"))))))));

  @Test
  void umSistemaInteiroAdaptadoAoNucleo() {
    var ana = new User("1", "Ana");
    var bruno = new User("2", "Bruno");
    var equipe = new Group("Equipe");
    var zelador = new Group("Zelador");
    MembershipManager.link(ana, equipe);
    MembershipManager.link(bruno, equipe);
    MembershipManager.link(bruno, zelador);

    var politicas = PolicyRepository.deDocumento(DOCUMENTO);
    equipe.anexar(politicas.porNome("Colaborador"));
    zelador.anexar(politicas.porNome("Faxina"));

    // ─────────── 5. montar o componente ───────────

    var iam = IamFactory.novo()
        .atributos(ATRIBUTOS)
        .catalogo(new ActionCatalog() {
          public Collection<Permission> todas() {
            return TUDO;
          }
        })
        .principais(new PrincipalDirectory() {
          public Collection<User> usuarios() {
            return List.of(ana, bruno);
          }

          public Collection<Group> grupos() {
            return List.of(equipe, zelador);
          }
        })
        .construir();

    var dela = new Arquivo("f1", ana.getId(), "20");
    var dele = new Arquivo("f2", bruno.getId(), "9000");
    var arquivos = List.of(dela, dele);

    // ─────────── e as cinco perguntas ───────────

    // 1. posso?
    assertTrue(iam.motor().isAllowed(ana, ESCREVER, dela));
    assertFalse(iam.motor().isAllowed(ana, ESCREVER, dele), "não é dela");
    assertTrue(iam.motor().isAllowed(ana, LER, dele), "ler é de todo mundo");

    // 2. por quê? — a cláusula que decidiu, e onde ela estava
    var negada = iam.motor().avaliar(ana, ESCREVER, dele);
    assertFalse(negada.permitido());
    assertEquals(poo.iam.Decisao.Tipo.NEGACAO_PADRAO, negada.getTipo());
    assertEquals("Equipe", iam.motor().avaliar(ana, ESCREVER, dela).getOrigem());

    // 3. o que posso fazer aqui? — em lote, para a interface esconder botões
    assertEquals(Map.of("LER", true, "ESCREVER", true, "APAGAR", true),
        iam.efetivas().sobre(ana, dela));
    assertEquals(Map.of("LER", true, "ESCREVER", false, "APAGAR", false),
        iam.efetivas().sobre(ana, dele));

    // 4. quem pode isto? — Bruno apaga o arquivo dele por ser dono, e o
    //    grande por ser zelador; Ana, nenhum dos dois
    assertEquals(List.of(bruno), iam.consultas().quemPode(APAGAR, dele).principais);
    assertEquals(List.of(ana), iam.consultas().quemPode(APAGAR, dela).principais);

    // 5. sobre o que posso agir? — e o mesmo filtro em SQL
    assertEquals(List.of(dela, dele), iam.consultas().filtrar(bruno, LER, arquivos));
    assertEquals(List.of(dele), iam.consultas().filtrar(bruno, APAGAR, arquivos));

    var sql = SqlWhereRenderer.render(iam.consultas().ondePosso(bruno, APAGAR), SQL);
    assertTrue(sql.contains("arquivo.dono_id = '2'"), sql);
    assertTrue(sql.contains("arquivo.bytes > 1000"), sql);
  }
}
