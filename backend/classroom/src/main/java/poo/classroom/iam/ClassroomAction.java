package poo.classroom.iam;

import poo.iam.Action;

/**
 * As ações desta aplicação. O núcleo não as conhece — ele só sabe que existe
 * uma {@link Action}.
 *
 * Repare que não há ação por papel: quem lista turmas é sempre
 * {@code LISTAR_TURMAS}, e o que muda entre administrador, professor e aluno é
 * a condição com que cada grupo recebe essa permissão.
 */
public enum ClassroomAction implements Action {
  LISTAR_TURMAS,
  VER_TURMA,
  LISTAR_ATIVIDADES,
  LISTAR_POSTS,
  LISTAR_COMENTARIOS,
  LISTAR_PARTICIPANTES,
  LISTAR_USUARIOS,
  VER_PERFIL,

  CRIAR_TURMA,
  CRIAR_ATIVIDADE,
  CRIAR_POST,
  CRIAR_COMENTARIO,
  CRIAR_PROFESSOR,
  CRIAR_ALUNO,

  EDITAR_TURMA,
  EDITAR_ATIVIDADE,
  EDITAR_POST,
  EDITAR_COMENTARIO,
  EDITAR_USUARIO,

  EXCLUIR_TURMA,
  EXCLUIR_ATIVIDADE,
  EXCLUIR_POST,
  EXCLUIR_COMENTARIO,
  EXCLUIR_USUARIO,

  MATRICULAR_ALUNO,
  DESMATRICULAR_ALUNO,
}
