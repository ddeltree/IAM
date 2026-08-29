package poo.api;

import poo.classroom.Turma;
import poo.classroom.iam.ClassroomConditions;
import poo.iam.ContextResolver;
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

  /**
   * Quem está na turma. A regra em si mora no módulo de autorização — aqui ela
   * é só reaproveitada pelo controller.
   */
  public static boolean isParticipante(User user, Turma turma) {
    if (user == null || turma == null)
      return false;
    var ctx = ContextResolver.padrao().resolver(user, turma);
    return ClassroomConditions.PARTICIPANTE.avaliar(ctx);
  }

}