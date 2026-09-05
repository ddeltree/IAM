import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Collection;
import java.util.LinkedHashMap;
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
import poo.iam.Policy;
import poo.iam.Resource;
import poo.iam.ResourceType;
import poo.iam.Statement;
import poo.iam.User;
import poo.iam.condition.Condition;
import poo.iam.query.PredicateRenderer;
import poo.iam.query.SqlWhereRenderer;
import poo.iam.spi.AttributeProvider;
import poo.iam.spi.PrincipalDirectory;
import poo.iam.spi.ResourcePolicyProvider;
import poo.iam.spi.SqlMapping;

/**
 * Compartilhar um objeto sem editar a política de ninguém.
 *
 * Com só política de identidade, "deixe o professor convidado ver esta turma"
 * obriga a mexer na política dele — e se forem cinco convidados em três turmas,
 * a política de identidade vira uma lista de exceções que ninguém consegue ler.
 * A política no recurso inverte: o dono do objeto concede sobre o objeto.
 *
 * O que este teste guarda de verdade é a metade cara: as <b>cinco consultas</b>
 * precisam enxergar essa concessão. Uma capacidade que decide certo e some das
 * respostas seria o modo mais silencioso de perder consultabilidade.
 */
class PoliticaNoRecursoTest {

  enum Acao implements Action {
    VER, EDITAR
  }

  enum Tipo implements ResourceType {
    DOCUMENTO
  }

  record Documento(String id, String donoId) implements Resource {
    public ResourceType getType() {
      return Tipo.DOCUMENTO;
    }

    public String getId() {
      return id;
    }
  }

  static final Permission VER = new Permission(Acao.VER, Tipo.DOCUMENTO);
  static final Permission EDITAR = new Permission(Acao.EDITAR, Tipo.DOCUMENTO);

  private Iam iam;
  private User ana;
  private User bruno;
  private Documento dela;
  private Documento dele;
  private final Map<String, Policy> politicasDeRecurso = new LinkedHashMap<>();
  private List<Documento> documentos;

  @BeforeEach
  void montar() {
    ana = new User("1", "Ana");
    bruno = new User("2", "Bruno");
    var todos = new Group("Todos");
    MembershipManager.link(ana, todos);
    MembershipManager.link(bruno, todos);

    // a política de identidade diz só uma coisa: você mexe no que é seu
    todos.grantPermission(VER, Condition.igual("recurso:donoId", "${principal:id}"));
    todos.grantPermission(EDITAR, Condition.igual("recurso:donoId", "${principal:id}"));

    dela = new Documento("d1", ana.getId());
    dele = new Documento("d2", bruno.getId());
    documentos = List.of(dela, dele);

    iam = IamFactory.novo()
        .atributos(new AttributeProvider() {
          public ResourceType tipo() {
            return Tipo.DOCUMENTO;
          }

          public Map<String, List<String>> atributosDe(Resource r) {
            return Map.of("donoId", List.of(((Documento) r).donoId()));
          }
        })
        .principais(new PrincipalDirectory() {
          public Collection<User> usuarios() {
            return List.of(ana, bruno);
          }

          public Collection<Group> grupos() {
            return List.of(todos);
          }
        })
        .politicasDeRecurso(new ResourcePolicyProvider() {
          public Policy politicaDe(Resource recurso) {
            return politicasDeRecurso.get(recurso.getId());
          }

          @Override
          public Collection<Resource> comPoliticaPropria(ResourceType tipo) {
            return documentos.stream()
                .filter(d -> politicasDeRecurso.containsKey(d.id()))
                .map(d -> (Resource) d)
                .toList();
          }
        })
        .construir();
  }

  /** Ana compartilha o documento dela com o Bruno, só para leitura. */
  private void compartilharComBruno() {
    politicasDeRecurso.put(dela.id(), new Policy("d1", Set.of(
        Statement.de(Effect.ALLOW, "VER", "DOCUMENTO/d1",
            Condition.igual("principal:id", bruno.getId())))));
  }

  @Test
  void aConcessaoValeSemQueAPoliticaDeNinguemMude() {
    assertFalse(iam.motor().isAllowed(bruno, VER, dela), "antes de compartilhar, não");

    compartilharComBruno();

    assertTrue(iam.motor().isAllowed(bruno, VER, dela));
    // e só o que foi compartilhado: ver, não editar
    assertFalse(iam.motor().isAllowed(bruno, EDITAR, dela));
    // nada na política do Bruno mudou
    assertTrue(bruno.getStatementsInline().isEmpty());
  }

  @Test
  void aDecisaoDizQueVeioDoRecurso() {
    compartilharComBruno();
    // sem isto, um acesso concedido por política de recurso seria
    // indistinguível de um concedido por grupo na hora de explicar
    assertEquals("DOCUMENTO/d1", iam.motor().avaliar(bruno, VER, dela).getOrigem());
  }

  @Test
  void aNegacaoNoRecursoVenceAConcessaoDeIdentidade() {
    // Ana é dona e o grupo concede; o documento nega
    politicasDeRecurso.put(dela.id(), new Policy("d1", Set.of(
        Statement.de(Effect.DENY, "EDITAR", "*", Condition.SEMPRE))));

    assertTrue(iam.motor().isAllowed(ana, VER, dela));
    assertFalse(iam.motor().isAllowed(ana, EDITAR, dela), "o dono foi barrado pelo próprio doc");
  }

  @Test
  void quemPodeEnxergaOCompartilhamento() {
    compartilharComBruno();

    var podado = Set.copyOf(iam.consultas().quemPode(VER, dela).principais);
    var varrido = Set.copyOf(iam.consultas().quemPodeVarrendo(VER, dela));

    assertEquals(varrido, podado, "a poda perdeu quem recebeu pela política do recurso");
    assertEquals(Set.of(ana, bruno), podado);
  }

  @Test
  void ondePossoEnxergaOCompartilhamento() {
    compartilharComBruno();

    // Bruno alcança o dele (por atributo) e o dela (por lista) — duas espécies
    // de concessão diferentes na mesma resposta
    var filtro = iam.consultas().ondePosso(bruno, VER);
    var predicado = PredicateRenderer.render(filtro, iam.contexto());
    assertTrue(predicado.test(dele));
    assertTrue(predicado.test(dela));

    assertEquals(List.of(dela, dele), iam.consultas().filtrar(bruno, VER, documentos));

    // e o filtro continua virando SQL, agora com um IN
    var sql = SqlWhereRenderer.render(filtro, new SqlMapping() {
      public String igual(String chave, String valor) {
        return chave.equals("recurso:donoId") ? "documento.dono_id = '" + valor + "'" : null;
      }

      public String contem(String chave, String valor) {
        return null;
      }
    });
    assertTrue(sql.contains("documento.dono_id = '2'"), sql);
    assertTrue(sql.contains("id IN ('d1')"), sql);
  }

  @Test
  void semCompartilhamentoNadaMuda() {
    // o provedor está lá, mas nenhum recurso tem política própria: a resposta
    // precisa ser exatamente a de antes
    assertEquals(List.of(dele), iam.consultas().filtrar(bruno, VER, documentos));
    assertEquals(Set.of(ana), Set.copyOf(iam.consultas().quemPode(VER, dela).principais));
  }
}
