package poo.classroom.iam;

import poo.iam.condition.Condition;

/**
 * As regras contextuais desta aplicação — o "sob quais circunstâncias" de cada
 * concessão.
 *
 * São dado, não código: cada uma é uma árvore de comparações sobre as chaves
 * que o {@link ClassroomAttributes} publica. É por serem dado que a política
 * pode ser impressa, explicada quando nega e consultada ao contrário.
 *
 * Nenhuma delas pergunta se quem chama é o administrador. Não precisa: o
 * administrador recebe as permissões dele sem condição nenhuma, e é isso que
 * torna essas regras legíveis.
 */
public final class ClassroomConditions {

  private ClassroomConditions() {
  }

  /** Quem responde pela turma do recurso. */
  public static final Condition PROFESSOR_RESPONSAVEL =
      Condition.igual("turma:professorId", "${principal:id}");

  /** Aluno matriculado na turma do recurso. */
  public static final Condition ALUNO_MATRICULADO =
      Condition.contem("turma:alunoIds", "${principal:id}");

  /**
   * Professor responsável ou aluno matriculado — quem "está na turma".
   *
   * A checagem de grupo acompanha a de vínculo porque é assim que a regra
   * sempre funcionou: ser o professor de uma turma só conta se você está no
   * grupo Professores.
   */
  public static final Condition PARTICIPANTE = Condition.algumaDas(
      Condition.todasAs(Condition.contem("principal:groups", "Professores"), PROFESSOR_RESPONSAVEL),
      Condition.todasAs(Condition.contem("principal:groups", "Alunos"), ALUNO_MATRICULADO));

  /** Quem escreveu a publicação ou o comentário. */
  public static final Condition AUTOR =
      Condition.igual("recurso:autorId", "${principal:id}");

  /** O recurso é o próprio usuário que está pedindo. */
  public static final Condition PROPRIO_USUARIO =
      Condition.igual("recurso:id", "${principal:id}");
}
