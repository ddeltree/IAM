package poo.iam;

public enum Action {

  LISTAR_TURMAS_ADM, // Apenas o ADMIN pode listar todas as turmas
  LISTAR_TURMAS_PROFESSOR, // PROFESSOR pode listar todas as turmas criadas por ele
  LISTAR_TURMAS_ALUNO, // apenas o ALUNO pode listar todas as turmas em que ele foi matriculado

  LISTAR_ATIVIDADES, // ADMIN e todos os PARTICIPANTES podem listar todas as atividades da turma
  LISTAR_POSTS, // ADMIN e todos os PARTICIPANTES podem listar todos os posts da turma
  LISTAR_COMENTARIOS, // ADMIN e todos os PARTICIPANTES podem listar todos os comentários
  LISTAR_PARTICIPANTES, // ADMIN e todos os PARTICIPANTES podem listar todos os participantes da turma;

  // ------------------------------------------------------------------------------------

  CRIAR_TURMA, // apenas o PROFESSOR
  CRIAR_ATIVIDADE, // apenas o PROFESSOR
  CRIAR_POST, // qualquer PARTICIPANTE
  CRIAR_COMENTARIO, // qualquer PARTICIPANTE
  CRIAR_PROFESSOR, // apenas o ADMIN
  CRIAR_ALUNO, // apenas o PROFESSOR

  // ------------------------------------------------------------------------------------

  EDITAR_TURMA, // apenas o PROFESSOR
  EDITAR_ATIVIDADE, // apenas o AUTOR (professor) ou o ADMIN
  EDITAR_POST, // apenas o AUTOR, o PROFESSOR ou o ADMIN
  EDITAR_COMENTARIO, // apenas o AUTOR, o PROFESSOR ou o ADMIN

  // ------------------------------------------------------------------------------------

  EXCLUIR_TURMA, // apenas o PROFESSOR
  EXCLUIR_ATIVIDADE, // apenas o AUTOR (professor) ou o ADMIN
  EXCLUIR_POST, // apenas o AUTOR, o PROFESSOR ou o ADMIN
  EXCLUIR_COMENTARIO, // apenas o AUTOR, o PROFESSOR ou o ADMIN
  EXCLUIR_USUARIO, // apenas o ADMIN

  // ------------------------------------------------------------------------------------

  MATRICULAR_ALUNO, // apenas o PROFESSOR
  DESMATRICULAR_ALUNO, // apenas o PROFESSOR
}

/*
 * MATRICULAR é inserir o aluno na turma
 * AUTOR é o professor ou aluno que criou a dada publicação ou comentário
 * PARTICIPANTE é o professor autor ou o aluno matriculado em uma turma
 * 
 */