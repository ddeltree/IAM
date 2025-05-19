package poo.iam;

import poo.iam.resources.ResourceTypes;

public enum SystemPermission {

  LISTAR_TURMAS_ADM(Action.LISTAR, ResourceTypes.TURMA),
  LISTAR_TURMAS_PROFESSOR(Action.LISTAR, ResourceTypes.TURMA),
  LISTAR_TURMAS_ALUNO(Action.LISTAR, ResourceTypes.TURMA),
  LISTAR_ATIVIDADES(Action.LISTAR, ResourceTypes.ATIVIDADE),
  LISTAR_POSTS(Action.LISTAR, ResourceTypes.POST),
  LISTAR_COMENTARIOS(Action.LISTAR, ResourceTypes.COMENTARIO),

  LISTAR_PARTICIPANTES(Action.LISTAR, ResourceTypes.USUARIO),
  LISTAR_USUARIOS(Action.LISTAR, ResourceTypes.USUARIO),
  VER_PERFIL(Action.VER, ResourceTypes.USUARIO),

  CRIAR_TURMA(Action.CRIAR, ResourceTypes.TURMA),
  CRIAR_ATIVIDADE(Action.CRIAR, ResourceTypes.ATIVIDADE),
  CRIAR_POST(Action.CRIAR, ResourceTypes.POST),
  CRIAR_COMENTARIO(Action.CRIAR, ResourceTypes.COMENTARIO),

  CRIAR_PROFESSOR(Action.CRIAR, ResourceTypes.USUARIO),
  CRIAR_ALUNO(Action.CRIAR, ResourceTypes.USUARIO),

  EDITAR_TURMA(Action.EDITAR, ResourceTypes.TURMA),
  EDITAR_ATIVIDADE(Action.EDITAR, ResourceTypes.ATIVIDADE),
  EDITAR_POST(Action.EDITAR, ResourceTypes.POST),
  EDITAR_COMENTARIO(Action.EDITAR, ResourceTypes.COMENTARIO),

  EXCLUIR_TURMA(Action.EXCLUIR, ResourceTypes.TURMA),
  EXCLUIR_ATIVIDADE(Action.EXCLUIR, ResourceTypes.ATIVIDADE),
  EXCLUIR_POST(Action.EXCLUIR, ResourceTypes.POST),
  EXCLUIR_COMENTARIO(Action.EXCLUIR, ResourceTypes.COMENTARIO),

  MATRICULAR_ALUNO(Action.MATRICULAR, ResourceTypes.USUARIO),
  DESMATRICULAR_ALUNO(Action.MATRICULAR, ResourceTypes.USUARIO);

  private final Permission permission;

  SystemPermission(Action action, ResourceTypes resource) {
    this.permission = new Permission(action, resource);
  }

  public Permission get() {
    return permission;
  }
}