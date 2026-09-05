import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.Test;

import poo.iam.Action;
import poo.iam.Effect;
import poo.iam.Group;
import poo.iam.IamFactory;
import poo.iam.MembershipManager;
import poo.iam.Permission;
import poo.iam.Policy;
import poo.iam.PrincipalResource;
import poo.iam.Statement;
import poo.iam.User;
import poo.iam.condition.Condition;
import poo.iam.document.PolicyDocument;
import poo.iam.document.PolicyRepository;

/**
 * A política deixa de morar no código.
 *
 * Escrever o documento já era possível, e valia menos do que parecia: uma
 * política que só se escreve continua sendo o código, e o documento é um
 * relatório sobre ele. Com a volta, o código passa a ser o motor e o documento
 * passa a ser a política — mudar quem pode o quê deixa de exigir recompilar.
 *
 * As políticas nomeadas são a outra metade: anexar em vez de copiar. Sem elas,
 * "os professores e os monitores podem o mesmo" se escreve duas vezes, e as
 * duas cópias envelhecem em ritmos diferentes.
 */
class PoliticaNomeadaTest {

  enum Acao implements Action {
    LER, ESCREVER
  }

  static final Permission LER = new Permission(Acao.LER, PrincipalResource.USUARIO);
  static final Permission ESCREVER = new Permission(Acao.ESCREVER, PrincipalResource.USUARIO);

  private static final poo.iam.Iam IAM = IamFactory.novo().construir();

  @Test
  void umaPoliticaAnexadaValeSemSerCopiada() {
    var leitura = new Policy("Leitura", Set.of(Statement.allow(LER)));
    var ana = new User("Ana");
    var bruno = new User("Bruno");

    ana.anexar(leitura);
    bruno.anexar(leitura);

    assertTrue(IAM.motor().isAllowed(ana, LER, null));
    assertTrue(IAM.motor().isAllowed(bruno, LER, null));

    // as cláusulas valem, mas não foram copiadas para dentro de ninguém
    assertTrue(ana.getStatementsInline().isEmpty());
    assertEquals(1, ana.getStatements().size());

    bruno.desanexar(leitura);
    assertFalse(IAM.motor().isAllowed(bruno, LER, null));
    assertTrue(IAM.motor().isAllowed(ana, LER, null), "desanexar de um não mexe no outro");
  }

  @Test
  void aPoliticaDeUmGrupoTambemPodeSerAnexada() {
    var equipe = new Group("Equipe");
    var ana = new User("Ana");
    MembershipManager.link(ana, equipe);

    equipe.anexar(new Policy("Escrita", Set.of(Statement.allow(ESCREVER))));

    // chega pelo grafo de principais, sem que o motor saiba que veio de uma
    // política nomeada anexada a um grupo
    assertTrue(IAM.motor().isAllowed(ana, ESCREVER, null));
    assertEquals("Equipe", IAM.motor().avaliar(ana, ESCREVER, null).getOrigem());
  }

  @Test
  void oDocumentoVaiEVolta() {
    var original = new Policy("Moderacao", Set.of(
        Statement.allow("*", "*"),
        Statement.de(Effect.DENY, "ESCREVER", "USUARIO/7",
            Condition.igual("recurso:autorId", "${principal:id}")).comSid("naoEditeOSeuProprio")));

    var relida = PolicyDocument.ler(PolicyDocument.escrever(original));

    assertEquals(original, relida, "a política mudou ao passar pelo documento");
    // o sid do autor sobrevive: é ele que a explicação de um 403 vai citar
    assertTrue(relida.getStatements().stream()
        .anyMatch(s -> s.getSid().equals("naoEditeOSeuProprio")));
  }

  @Test
  void umDocumentoMalformadoFalhaAltoEmVezDeSilenciar() {
    // ignorar o campo que não entendeu produziria uma política silenciosamente
    // mais permissiva ou mais restritiva que a escrita — o tipo de erro que só
    // aparece quando alguém não consegue fazer o trabalho
    assertThrows(IllegalArgumentException.class,
        () -> PolicyDocument.ler(java.util.Map.of("statements", List.of())),
        "política sem nome");

    assertThrows(IllegalArgumentException.class,
        () -> PolicyDocument.ler(java.util.Map.of("nome", "X",
            "statements", List.of(java.util.Map.of("effect", "ALLOW")))),
        "cláusula sem action");

    assertThrows(IllegalArgumentException.class,
        () -> PolicyRepository.emMemoria(List.of(
            new Policy("Igual", Set.of()), new Policy("Igual", Set.of()))),
        "duas políticas com o mesmo nome: a segunda apagaria a primeira");
  }

  @Test
  void anexarUmaPoliticaInexistenteEErroENaoAusenciaDePermissao() {
    var repo = PolicyRepository.emMemoria(List.of(new Policy("Existe", Set.of())));
    // devolver null faria a aplicação subir com todo mundo barrado, sem dizer
    // por quê — é pior do que não subir
    assertThrows(IllegalArgumentException.class, () -> repo.porNome("NaoExiste"));
    assertEquals("Existe", repo.porNome("Existe").getNome());
  }
}
