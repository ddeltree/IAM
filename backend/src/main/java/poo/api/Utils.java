package poo.api;

import poo.iam.SecurityContext;

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
}
