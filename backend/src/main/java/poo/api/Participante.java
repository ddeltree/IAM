package poo.api;

import poo.classroom.Turma;
import poo.iam.SecurityContext;
import poo.iam.User;

public class Participante {
  public String turmaId;
  public String userId;
  public String name;

  public Participante(User user, Turma turma) {
    this.turmaId = turma.getId();
    this.userId = user.getId();
    this.name = user.getName();
  }

  public static boolean isParticipante(String uid, String turmaId) {
    var turma = TurmaController.getTurma(turmaId);
    var auth = SecurityContext.getInstance();
    var user = UserController.getUser(uid);
    if (turma == null || user == null)
      return false;
    var isAutorTurma = auth.isProfessor(user)
        && turma.getProfessorResponsavel().getId().equals(user.getId());
    var isAlunoTurma = auth.isAluno(user) && turma.temAluno(user);
    return isAutorTurma || isAlunoTurma;
  }

}