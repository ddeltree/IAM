import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import poo.iam.Action;
import poo.iam.Group;
import poo.iam.Iam;
import poo.iam.IamFactory;
import poo.iam.MembershipManager;
import poo.iam.Permission;
import poo.iam.Resource;
import poo.iam.ResourceType;
import poo.iam.Role;
import poo.iam.Statement;
import poo.iam.User;
import poo.iam.condition.Condition;
import poo.iam.spi.AttributeProvider;
import poo.iam.spi.PrincipalDirectory;

/**
 * Um papel não é um grupo.
 *
 * A política de um grupo vale o tempo todo para quem está nele. A de um papel
 * só vale enquanto alguém o está exercendo, e essa pessoa continua sendo ela
 * mesma fora dali. É o que separa "o professor é moderador" de "o professor
 * pode passar a moderar quando precisar" — e a diferença aparece no registro:
 * uma exclusão feita sob o papel se distingue de uma feita como professor.
 */
class PapelESessaoTest {

  enum Acao implements Action {
    EDITAR, EXCLUIR
  }

  enum Tipo implements ResourceType {
    POST
  }

  record Post(String id, String autorId) implements Resource {
    public ResourceType getType() {
      return Tipo.POST;
    }

    public String getId() {
      return id;
    }
  }

  static final Permission EDITAR = new Permission(Acao.EDITAR, Tipo.POST);
  static final Permission EXCLUIR = new Permission(Acao.EXCLUIR, Tipo.POST);

  private Iam iam;
  private User professor;
  private User aluno;
  private Role moderador;
  private Post doAluno;

  @BeforeEach
  void montar() {
    professor = new User("1", "Professor");
    aluno = new User("2", "Aluno");
    var professores = new Group("Professores");
    MembershipManager.link(professor, professores);

    // como si mesmo, cada um só mexe no que é seu
    var autor = Condition.igual("recurso:autorId", "${principal:id}");
    professores.grantPermission(EDITAR, autor);
    aluno.grantPermission(EDITAR, autor);

    // o papel de moderador exclui qualquer post — e só professores o assumem
    moderador = new Role("Moderador");
    moderador.add(Statement.allow(EXCLUIR));
    moderador.confiaEm(Condition.contem("principal:groups", "Professores"));

    doAluno = new Post("p1", aluno.getId());

    iam = IamFactory.novo()
        .atributos(new AttributeProvider() {
          public ResourceType tipo() {
            return Tipo.POST;
          }

          public Map<String, List<String>> atributosDe(Resource r) {
            return Map.of("autorId", List.of(((Post) r).autorId()));
          }
        })
        .principais(new PrincipalDirectory() {
          public Collection<User> usuarios() {
            return List.of(professor, aluno);
          }

          public Collection<Group> grupos() {
            return List.of(professores);
          }

          @Override
          public Collection<Role> papeis() {
            return List.of(moderador);
          }
        })
        .construir();
  }

  @Test
  void oPapelSoValeEnquantoEstiverSendoExercido() {
    assertFalse(iam.motor().isAllowed(professor, EXCLUIR, doAluno),
        "como si mesmo, o professor não exclui post alheio");

    var sessao = iam.papeis().assumir(professor, moderador);
    assertTrue(iam.motor().isAllowed(sessao, EXCLUIR, doAluno));

    // e largar a sessão é simplesmente parar de usá-la: nada mudou no professor
    assertFalse(iam.motor().isAllowed(professor, EXCLUIR, doAluno));
    assertTrue(professor.getStatementsInline().isEmpty());
  }

  @Test
  void aConfiancaDoPapelDizQuemPodeAssumiLo() {
    assertNull(iam.papeis().assumir(aluno, moderador), "aluno não é professor");
    assertFalse(iam.papeis().podeAssumir(aluno, moderador).permitido());
    assertTrue(iam.papeis().podeAssumir(professor, moderador).permitido());
  }

  @Test
  void aSessaoDizQuemEstavaPorTras() {
    var sessao = iam.papeis().assumir(professor, moderador);

    // principal:id passa a ser o da sessão, como o aws:userid na AWS
    var ctx = iam.contexto().resolver(sessao, doAluno);
    assertEquals(List.of(sessao.getId()), ctx.get("principal:id"));
    assertEquals(List.of("Moderador"), ctx.get("sessao:papel"));
    assertEquals(List.of(professor.getId()), ctx.get("sessao:origem"));

    // e é isso que permite "o moderador age em tudo, menos no que é dele"
    var doProfessor = new Post("p2", professor.getId());
    moderador.add(Statement.deny(EXCLUIR,
        Condition.igual("recurso:autorId", "${sessao:origem}")));
    assertFalse(iam.motor().isAllowed(sessao, EXCLUIR, doProfessor));
    assertTrue(iam.motor().isAllowed(sessao, EXCLUIR, doAluno));
  }

  @Test
  void umaCondicaoSobreOAutorNaoValeSobOPapel() {
    // sob a sessão, principal:id é o da sessão — então "o autor pode editar"
    // deixa de valer. Está certo, e é o que separa agir como si mesmo de agir
    // sob um papel
    var seuProprio = new Post("p3", professor.getId());
    assertTrue(iam.motor().isAllowed(professor, EDITAR, seuProprio));

    var sessao = iam.papeis().assumir(professor, moderador);
    assertFalse(iam.motor().isAllowed(sessao, EDITAR, seuProprio),
        "a sessão não é a pessoa, e a condição do autor sabe disso");
  }

  @Test
  void quemPodeSeparaOsDiretosDosQueChegariamPeloPapel() {
    var resultado = iam.consultas().quemPode(EXCLUIR, doAluno);

    assertEquals(List.of(), resultado.principais, "ninguém exclui post alheio agora");
    assertEquals(Set.of("Moderador"), resultado.viaPapel.keySet());
    assertEquals(List.of(professor), resultado.viaPapel.get("Moderador"),
        "só quem tem confiança para assumir o papel");

    // juntar os dois seria superestimar o acesso atual numa auditoria
    assertFalse(resultado.principais.contains(professor));
  }

  @Test
  void semPapeisNadaMuda() {
    var semPapeis = IamFactory.novo()
        .principais(new PrincipalDirectory() {
          public Collection<User> usuarios() {
            return List.of(professor);
          }

          public Collection<Group> grupos() {
            return List.of();
          }
        })
        .construir();
    assertTrue(semPapeis.consultas().quemPode(EXCLUIR, doAluno).viaPapel.isEmpty());
  }
}
