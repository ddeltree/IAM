package poo.classroom.iam;

import poo.classroom.Atividade;
import poo.classroom.Comentario;
import poo.classroom.Publicacao;
import poo.classroom.Turma;
import poo.iam.PermissionCondition;
import poo.iam.Resource;
import poo.iam.User;

/**
 * As regras contextuais desta aplicação — o "sob quais circunstâncias" de cada
 * concessão.
 *
 * Nenhuma delas pergunta se quem chama é o administrador. Não precisa: o
 * administrador recebe as permissões dele sem condição nenhuma, e é isso que
 * torna essas regras legíveis.
 */
public final class ClassroomConditions {

  private ClassroomConditions() {
  }

  /** Quem responde pela turma do recurso. */
  public static final PermissionCondition PROFESSOR_RESPONSAVEL = (user, resource, ctx) -> {
    var turma = turmaDe(resource);
    return turma != null && turma.getProfessorResponsavel().equals(user);
  };

  /** Aluno matriculado na turma do recurso. */
  public static final PermissionCondition ALUNO_MATRICULADO = (user, resource, ctx) -> {
    var turma = turmaDe(resource);
    return turma != null && turma.temAluno(user.getId());
  };

  /** Professor responsável ou aluno matriculado — quem "está na turma". */
  public static final PermissionCondition PARTICIPANTE = (user, resource, ctx) -> {
    var turma = turmaDe(resource);
    if (turma == null)
      return false;
    var auth = SecurityContext.getInstance();
    var ehProfessorDaTurma = auth.isProfessor(user) && turma.getProfessorResponsavel().equals(user);
    var ehAlunoDaTurma = auth.isAluno(user) && turma.temAluno(user);
    return ehProfessorDaTurma || ehAlunoDaTurma;
  };

  /** Quem escreveu a publicação ou o comentário. */
  public static final PermissionCondition AUTOR = (user, resource, ctx) -> {
    if (resource instanceof Publicacao)
      return user.equals(((Publicacao) resource).getAutor());
    if (resource instanceof Comentario)
      return user.equals(((Comentario) resource).getAutor());
    return false;
  };

  /** O recurso é o próprio usuário que está pedindo. */
  public static final PermissionCondition PROPRIO_USUARIO = (user, resource, ctx) -> {
    return resource instanceof User && user.equals(resource);
  };

  /**
   * A turma à qual o recurso pertence. Aceitar turma, publicação ou comentário
   * é o que permite usar {@code PROFESSOR_RESPONSAVEL} em todos eles sem
   * escrever uma condição por tipo.
   */
  private static Turma turmaDe(Resource resource) {
    if (resource instanceof Turma)
      return (Turma) resource;
    if (resource instanceof Atividade)
      return ((Atividade) resource).getTurma();
    if (resource instanceof Publicacao)
      return ((Publicacao) resource).getTurma();
    if (resource instanceof Comentario)
      return ((Comentario) resource).getPublicacao().getTurma();
    return null;
  }
}
