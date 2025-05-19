package poo.api;

import poo.iam.PermissionService;
import poo.iam.SecurityContext;
import poo.iam.SystemPermission;
import poo.iam.User;
import io.javalin.http.Context;

public class Utils {
  public static boolean isParticipante(String uid, String turmaId) {
    var isAdmin = isAdmin(uid);
    var turma = TurmaController.getTurma(turmaId);
    var auth = SecurityContext.getInstance();
    var admin = auth.getAdmin();
    var user = isAdmin ? admin : UserController.getUser(uid);
    var isAutorTurma = turma != null && auth.isProfessor(user)
        && turma.getProfessorResponsavel().getId().equals(user.getId());
    var isAlunoTurma = turma != null && auth.isAluno(user) && turma.temAluno(user);
    return isAutorTurma || isAlunoTurma;
  }

  public static boolean isAdmin(String uid) {
    var auth = SecurityContext.getInstance();
    var admin = auth.getAdmin();
    var user = uid.equals(admin.getId()) ? admin : UserController.getUser(uid);
    var isAdmin = auth.isAdmin(user);
    return isAdmin;
  }

  public static User findUserOr404(Context ctx) {
    var uid = ctx.cookie("uid");
    User user = UserController.getUser(uid);
    if (user == null) {
      ctx.status(404).result("Usuário não encontrado");
      return null;
    }
    return user;
  }

  public static boolean hasPermissionOr403(SystemPermission permission, Context ctx) {
    var user = findUserOr404(ctx);
    if (!PermissionService.hasPermission(user, permission.get())) {
      ctx.status(403).result("Não autorizado");
      return false;
    } else {
      return true;
    }
  }
}
