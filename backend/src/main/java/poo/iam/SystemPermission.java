package poo.iam;

import poo.iam.resources.ResourceTypes;

public enum SystemPermission {
  // Apenas o ADMIN pode listar todas as turmas
  LISTAR_TURMAS_ADM(Action.LISTAR, ResourceTypes.TURMA),
  // PROFESSOR pode listar todas as turmas criadas por ele
  LISTAR_TURMAS_PROFESSOR(Action.LISTAR, ResourceTypes.TURMA),
  // apenas o ALUNO pode listar todas as turmas em que ele foi matriculado
  LISTAR_TURMAS_ALUNO(Action.LISTAR, ResourceTypes.TURMA),

  // ADMIN e todos os PARTICIPANTES podem listar todas as atividades da turma
  LISTAR_ATIVIDADES(Action.LISTAR, ResourceTypes.ATIVIDADE),
  // ADMIN e todos os PARTICIPANTES podem listar todos os posts da turma
  LISTAR_POSTS(Action.LISTAR, ResourceTypes.POST),
  // ADMIN e todos os PARTICIPANTES podem listar todos os comentários
  LISTAR_COMENTARIOS(Action.LISTAR, ResourceTypes.COMENTARIO),

  // ADMIN e todos os PARTICIPANTES podem listar todos os participantes da turma;
  LISTAR_PARTICIPANTES(Action.LISTAR, ResourceTypes.USUARIO),
  // apenas o ADMIN pode listar todos os usuarios
  LISTAR_USUARIOS(Action.LISTAR, ResourceTypes.USUARIO),
  // apenas o usuário logado pode ver seu perfil
  VER_PERFIL(Action.VER, ResourceTypes.USUARIO),

  // apenas o PROFESSOR
  CRIAR_TURMA(Action.CRIAR, ResourceTypes.TURMA),
  // apenas o PROFESSOR
  CRIAR_ATIVIDADE(Action.CRIAR, ResourceTypes.ATIVIDADE),
  // qualquer PARTICIPANTE
  CRIAR_POST(Action.CRIAR, ResourceTypes.POST),
  // qualquer PARTICIPANTE
  CRIAR_COMENTARIO(Action.CRIAR, ResourceTypes.COMENTARIO),
  // apenas o ADMIN
  CRIAR_PROFESSOR(Action.CRIAR, ResourceTypes.USUARIO),
  // apenas o PROFESSOR
  CRIAR_ALUNO(Action.CRIAR, ResourceTypes.USUARIO),

  // apenas o PROFESSOR AUTOR
  EDITAR_TURMA(Action.EDITAR, ResourceTypes.TURMA),
  // apenas o PROFESSOR AUTOR ou o ADMIN
  EDITAR_ATIVIDADE(Action.EDITAR, ResourceTypes.ATIVIDADE),
  // apenas o AUTOR, o PROFESSOR ou o ADMIN
  EDITAR_POST(Action.EDITAR, ResourceTypes.POST),
  // apenas o AUTOR, o PROFESSOR ou o ADMIN
  EDITAR_COMENTARIO(Action.EDITAR, ResourceTypes.COMENTARIO),
  // apenas o próprio USUÁRIO
  EDITAR_USUARIO(Action.EDITAR, ResourceTypes.USUARIO),

  // apenas o PROFESSOR
  EXCLUIR_TURMA(Action.EXCLUIR, ResourceTypes.TURMA),
  // apenas o AUTOR (professor) ou o ADMIN
  EXCLUIR_ATIVIDADE(Action.EXCLUIR, ResourceTypes.ATIVIDADE),
  // apenas o AUTOR, o PROFESSOR ou o ADMIN
  EXCLUIR_POST(Action.EXCLUIR, ResourceTypes.POST),
  // apenas o AUTOR, o PROFESSOR ou o ADMIN
  EXCLUIR_COMENTARIO(Action.EXCLUIR, ResourceTypes.COMENTARIO),
  // apenas o ADMIN e o próprio USUÁRIO
  EXCLUIR_USUARIO(Action.EXCLUIR, ResourceTypes.USUARIO),

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

/*
 * MATRICULAR é inserir o aluno na turma
 * AUTOR é o professor ou aluno que criou a dada publicação ou comentário
 * PARTICIPANTE é o professor autor ou o aluno matriculado em uma turma
 * 
 */