import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Map;

import org.junit.jupiter.api.Test;

import poo.api.UserController;
import poo.api.exceptions.ForbiddenException;
import poo.iam.Group;
import poo.iam.MembershipManager;
import poo.iam.SecurityContext;
import poo.iam.SystemPermission;
import poo.iam.User;

/**
 * Resolução de permissões: concessão inline, herança de grupo e a negação
 * explícita, que sobrepõe as duas ("deny overrides").
 */
public class PermissoesTest extends ApiFixture {

  private static final SystemPermission PERM = SystemPermission.LISTAR_USUARIOS;

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

  @Test
  void negarNoGrupoProfessoresBloqueiaACriacaoDeTurmas() {
    test((server, client) -> {
      criar2Professores2Alunos(client);
      var professores = SecurityContext.getInstance().getProfessores();

      assertEquals(201, POST(client, "/turmas", PROF1_ID, Map.of("nome", "POO")).code());

      professores.denyPermission(SystemPermission.CRIAR_TURMA.get());
      assertEquals(ForbiddenException.STATUS_CODE,
          POST(client, "/turmas", PROF1_ID, Map.of("nome", "POO II")).code());

      professores.allowPermission(SystemPermission.CRIAR_TURMA.get());
      assertEquals(201, POST(client, "/turmas", PROF1_ID, Map.of("nome", "POO III")).code());
    });
  }

  @Test
  void negacaoIndividualNaoAtingeOsColegasDeGrupo() {
    test((server, client) -> {
      criar2Professores2Alunos(client);
      var prof1 = UserController.getUser(String.valueOf(PROF1_ID));
      prof1.denyPermission(SystemPermission.CRIAR_TURMA.get());

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
      SecurityContext.getInstance().getProfessores().denyPermission(SystemPermission.CRIAR_TURMA.get());
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
