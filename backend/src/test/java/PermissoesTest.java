import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Map;

import org.junit.jupiter.api.Test;

import poo.api.UserController;
import poo.api.exceptions.ForbiddenException;
import poo.iam.Decisao;
import poo.iam.Group;
import poo.iam.MembershipManager;
import poo.iam.PermissionCondition;
import poo.classroom.iam.SecurityContext;
import poo.classroom.iam.ClassroomPermission;
import poo.iam.User;

/**
 * Resolução de permissões: concessão inline, herança de grupo e a negação
 * explícita, que sobrepõe as duas ("deny overrides").
 */
public class PermissoesTest extends ApiFixture {

  private static final ClassroomPermission PERM = ClassroomPermission.LISTAR_USUARIOS;

  @Test
  void semConcessaoNaoTemAcesso() {
    assertFalse(PERM.isAllowed(new User("sem permissões")));
  }

  @Test
  void concessaoInlineDaAcesso() {
    var user = new User("inline");
    user.grantPermission(PERM.get());
    assertTrue(PERM.isAllowed(user));

    user.revokePermission(PERM.get());
    assertFalse(PERM.isAllowed(user));
  }

  @Test
  void permissaoEHerdadaDoGrupo() {
    var user = new User("membro");
    var grupo = new Group("grupo");
    grupo.grantPermission(PERM.get());
    MembershipManager.link(user, grupo);
    assertTrue(PERM.isAllowed(user));

    // ao sair do grupo, a herança some
    MembershipManager.unlink(user, grupo);
    assertFalse(PERM.isAllowed(user));
  }

  @Test
  void negacaoNoUsuarioVenceAHerancaDoGrupo() {
    var user = new User("negado");
    var grupo = new Group("grupo");
    grupo.grantPermission(PERM.get());
    MembershipManager.link(user, grupo);

    user.denyPermission(PERM.get());
    assertFalse(PERM.isAllowed(user));
  }

  @Test
  void negacaoNoGrupoAtingeOsMembros() {
    var user = new User("membro");
    var grupo = new Group("grupo");
    grupo.grantPermission(PERM.get());
    MembershipManager.link(user, grupo);

    grupo.denyPermission(PERM.get());
    assertFalse(PERM.isAllowed(user));
  }

  @Test
  void negacaoVenceConcessaoNoMesmoUsuario() {
    var user = new User("os dois");
    user.grantPermission(PERM.get());
    user.denyPermission(PERM.get());
    assertFalse(PERM.isAllowed(user));

    // a ordem inversa dá no mesmo: a negação não é apagada por uma concessão
    var outro = new User("ordem inversa");
    outro.denyPermission(PERM.get());
    outro.grantPermission(PERM.get());
    assertFalse(PERM.isAllowed(outro));
  }

  @Test
  void allowRemoveANegacaoMasNaoConcedeNada() {
    var user = new User("sem grant");
    user.denyPermission(PERM.get());
    user.allowPermission(PERM.get());
    // a negação saiu, mas nada foi concedido
    assertFalse(PERM.isAllowed(user));

    var membro = new User("com grupo");
    var grupo = new Group("grupo");
    grupo.grantPermission(PERM.get());
    MembershipManager.link(membro, grupo);
    membro.denyPermission(PERM.get());
    membro.allowPermission(PERM.get());
    // com a negação removida, a herança do grupo volta a valer
    assertTrue(PERM.isAllowed(membro));
  }

  @Test
  void negacaoApareceNaListaDoHolder() {
    var user = new User("listagem");
    user.denyPermission(PERM.get());

    assertTrue(user.getDeniedPermissions().contains(PERM.get()));
    assertFalse(user.getInlinePermissions().contains(PERM.get()));
  }



  // --- decisão explicada ------------------------------------------------
  //
  // Saber que deu 403 não basta: o motor diz qual cláusula decidiu e onde ela
  // estava. É o equivalente ao MatchedStatements do policy simulator da AWS.

  @Test
  void concessaoDeGrupoDizDeQualGrupoVeio() {
    var user = new User("membro");
    var grupo = new Group("Professores");
    grupo.grantPermission(PERM.get());
    MembershipManager.link(user, grupo);

    var decisao = PERM.avaliar(user, null);
    assertTrue(decisao.permitido());
    assertEquals(Decisao.Tipo.PERMITIDO, decisao.getTipo());
    assertEquals("Professores", decisao.getOrigem());
    assertEquals("ALLOW:LISTAR_USUARIOS:USUARIO", decisao.getDecisiva().getSid());
  }

  @Test
  void negacaoExplicitaSeDistingueDaNegacaoPadrao() {
    var semNada = new User("sem nada");
    var negado = new User("negado");
    negado.denyPermission(PERM.get());

    // ninguém concedeu
    var padrao = PERM.avaliar(semNada, null);
    assertEquals(Decisao.Tipo.NEGACAO_PADRAO, padrao.getTipo());
    assertNull(padrao.getDecisiva());

    // alguém negou de propósito
    var explicita = PERM.avaliar(negado, null);
    assertEquals(Decisao.Tipo.NEGACAO_EXPLICITA, explicita.getTipo());
    assertEquals("inline", explicita.getOrigem());
    assertEquals("DENY:LISTAR_USUARIOS:USUARIO", explicita.getDecisiva().getSid());
  }

  @Test
  void negacaoDeGrupoApontaOGrupoQueBarrou() {
    var user = new User("barrado");
    var concede = new Group("Concede");
    var barra = new Group("Barra");
    concede.grantPermission(PERM.get());
    barra.denyPermission(PERM.get());
    MembershipManager.link(user, concede);
    MembershipManager.link(user, barra);

    var decisao = PERM.avaliar(user, null);
    assertFalse(decisao.permitido());
    assertEquals(Decisao.Tipo.NEGACAO_EXPLICITA, decisao.getTipo());
    assertEquals("Barra", decisao.getOrigem());
  }

  // --- concessões condicionais -------------------------------------------
  //
  // A condição pertence à concessão, e não à ação. É isso que permite conceder
  // a mesma permissão com restrições diferentes a principais diferentes.

  /** Só permite quando o recurso é o próprio usuário que está pedindo. */
  private static final PermissionCondition SOBRE_SI_MESMO = (user, resource, ctx) -> user.equals(resource);

  @Test
  void concessaoCondicionalSoValeQuandoACondicaoPassa() {
    var user = new User("condicional");
    var outro = new User("outro");
    user.grantPermission(PERM.get(), SOBRE_SI_MESMO);

    assertTrue(PERM.isAllowed(user, user));
    assertFalse(PERM.isAllowed(user, outro));
  }

  @Test
  void mesmaPermissaoComCondicoesDiferentesEmCadaGrupo() {
    var irrestrito = new Group("irrestrito");
    irrestrito.grantPermission(PERM.get());
    var restrito = new Group("restrito");
    restrito.grantPermission(PERM.get(), SOBRE_SI_MESMO);

    var moderador = new User("moderador");
    MembershipManager.link(moderador, irrestrito);
    var comum = new User("comum");
    MembershipManager.link(comum, restrito);

    var alvo = new User("alvo");
    // o moderador alcança qualquer recurso; o comum, só a si mesmo
    assertTrue(PERM.isAllowed(moderador, alvo));
    assertFalse(PERM.isAllowed(comum, alvo));
    assertTrue(PERM.isAllowed(comum, comum));
  }

  @Test
  void concessoesDeGruposDiferentesSeSomam() {
    var user = new User("somatorio");
    var g1 = new Group("g1");
    var g2 = new Group("g2");
    g1.grantPermission(PERM.get(), SOBRE_SI_MESMO);
    g2.grantPermission(PERM.get());
    MembershipManager.link(user, g1);
    MembershipManager.link(user, g2);

    // a concessão irrestrita de g2 cobre o que a condição de g1 barraria
    assertTrue(PERM.isAllowed(user, new User("qualquer")));
  }

  @Test
  void negacaoCondicionalSoDerrubaOCasoQueEspecifica() {
    var user = new User("negado em parte");
    user.grantPermission(PERM.get());
    user.denyPermission(PERM.get(), SOBRE_SI_MESMO);

    // a negação vale só sobre si mesmo; nos demais recursos a concessão fica de pé
    assertFalse(PERM.isAllowed(user, user));
    assertTrue(PERM.isAllowed(user, new User("terceiro")));
  }

  @Test
  void negarNoGrupoProfessoresBloqueiaACriacaoDeTurmas() {
    test((server, client) -> {
      criar2Professores2Alunos(client);
      var professores = SecurityContext.getInstance().getProfessores();

      assertEquals(201, POST(client, "/turmas", PROF1_ID, Map.of("nome", "POO")).code());

      professores.denyPermission(ClassroomPermission.CRIAR_TURMA.get());
      assertEquals(ForbiddenException.STATUS_CODE,
          POST(client, "/turmas", PROF1_ID, Map.of("nome", "POO II")).code());

      professores.allowPermission(ClassroomPermission.CRIAR_TURMA.get());
      assertEquals(201, POST(client, "/turmas", PROF1_ID, Map.of("nome", "POO III")).code());
    });
  }

  @Test
  void negacaoIndividualNaoAtingeOsColegasDeGrupo() {
    test((server, client) -> {
      criar2Professores2Alunos(client);
      var prof1 = UserController.getUser(String.valueOf(PROF1_ID));
      prof1.denyPermission(ClassroomPermission.CRIAR_TURMA.get());

      assertEquals(ForbiddenException.STATUS_CODE,
          POST(client, "/turmas", PROF1_ID, Map.of("nome", "POO")).code());
      // PROF2 continua com a permissão herdada do grupo
      assertEquals(201, POST(client, "/turmas", PROF2_ID, Map.of("nome", "Cálculo")).code());
    });
  }

  @Test
  void resetRestauraAsPermissoesPadrao() {
    test((server, client) -> {
      criar2Professores2Alunos(client);
      SecurityContext.getInstance().getProfessores().denyPermission(ClassroomPermission.CRIAR_TURMA.get());
      assertEquals(ForbiddenException.STATUS_CODE,
          POST(client, "/turmas", PROF1_ID, Map.of("nome", "POO")).code());
    });

    // cenário novo: o reset precisa ter desfeito a negação acima
    test((server, client) -> {
      criar2Professores2Alunos(client);
      assertEquals(201, POST(client, "/turmas", PROF1_ID, Map.of("nome", "POO")).code());
    });
  }
}
