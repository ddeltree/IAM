package poo.iam;

import poo.iam.resources.ResourceTypes;

public enum SystemPermission {
  LISTAR_TURMAS_ADM(Action.LISTAR_TURMAS_ADM, ResourceTypes.TURMA),
  LISTAR_TURMAS_PROFESSOR(Action.LISTAR_TURMAS_PROFESSOR, ResourceTypes.TURMA),
  LISTAR_TURMAS_ALUNO(Action.LISTAR_TURMAS_ALUNO, ResourceTypes.TURMA),
  LISTAR_ATIVIDADES(Action.LISTAR_ATIVIDADES, ResourceTypes.ATIVIDADE),
  LISTAR_POSTS(Action.LISTAR_POSTS, ResourceTypes.POST),
  LISTAR_COMENTARIOS(Action.LISTAR_COMENTARIOS, ResourceTypes.COMENTARIO),
  LISTAR_PARTICIPANTES(Action.LISTAR_PARTICIPANTES, ResourceTypes.USUARIO),

  CRIAR_TURMA(Action.CRIAR_TURMA, ResourceTypes.TURMA),
  CRIAR_ATIVIDADE(Action.CRIAR_ATIVIDADE, ResourceTypes.ATIVIDADE),
  CRIAR_POST(Action.CRIAR_POST, ResourceTypes.POST),
  CRIAR_COMENTARIO(Action.CRIAR_COMENTARIO, ResourceTypes.COMENTARIO),
  CRIAR_PROFESSOR(Action.CRIAR_PROFESSOR, ResourceTypes.USUARIO),
  CRIAR_ALUNO(Action.CRIAR_ALUNO, ResourceTypes.USUARIO),

  EDITAR_TURMA(Action.EDITAR_TURMA, ResourceTypes.TURMA),
  EDITAR_ATIVIDADE(Action.EDITAR_ATIVIDADE, ResourceTypes.ATIVIDADE),
  EDITAR_POST(Action.EDITAR_POST, ResourceTypes.POST),
  EDITAR_COMENTARIO(Action.EDITAR_COMENTARIO, ResourceTypes.COMENTARIO),

  EXCLUIR_TURMA(Action.EXCLUIR_TURMA, ResourceTypes.TURMA),
  EXCLUIR_ATIVIDADE(Action.EXCLUIR_ATIVIDADE, ResourceTypes.ATIVIDADE),
  EXCLUIR_POST(Action.EXCLUIR_POST, ResourceTypes.POST),
  EXCLUIR_COMENTARIO(Action.EXCLUIR_COMENTARIO, ResourceTypes.COMENTARIO),
  // EXCLUIR_USUARIO(Action.EXCLUIR_USUARIO, ResourceTypes.USUARIO),

  MATRICULAR_ALUNO(Action.MATRICULAR_ALUNO, ResourceTypes.USUARIO),
  DESMATRICULAR_ALUNO(Action.DESMATRICULAR_ALUNO, ResourceTypes.USUARIO);

  private final Permission permission;

  SystemPermission(Action action, ResourceTypes resource) {
    this.permission = new Permission(action, resource);
  }

  public Permission get() {
    return permission;
  }
}
