package poo.classroom.iam;

import static poo.classroom.iam.ClassroomAction.*;
import static poo.classroom.iam.ClassroomResource.*;

import poo.iam.AccessResolver;
import poo.iam.Action;
import poo.iam.Permission;
import poo.iam.PrincipalResource;
import poo.iam.Resource;
import poo.iam.ResourceType;
import poo.iam.User;

/**
 * Catálogo das permissões desta aplicação: o par (ação, tipo de recurso) que os
 * controllers usam para pedir autorização.
 *
 * Aqui não há condição nenhuma — ela pertence à concessão, e é o
 * {@link SecurityContext} que decide com que restrição cada grupo recebe cada
 * permissão. O tipo de recurso declarado é o do objeto que realmente chega na
 * verificação: listar posts é uma ação sobre a TURMA que os contém.
 */
public enum ClassroomPermission {
  LISTAR_TURMAS(ClassroomAction.LISTAR_TURMAS, TURMA),
  VER_TURMA(ClassroomAction.VER_TURMA, TURMA),
  LISTAR_ATIVIDADES(ClassroomAction.LISTAR_ATIVIDADES, TURMA),
  LISTAR_POSTS(ClassroomAction.LISTAR_POSTS, TURMA),
  LISTAR_COMENTARIOS(ClassroomAction.LISTAR_COMENTARIOS, TURMA),
  LISTAR_PARTICIPANTES(ClassroomAction.LISTAR_PARTICIPANTES, TURMA),
  LISTAR_USUARIOS(ClassroomAction.LISTAR_USUARIOS, PrincipalResource.USUARIO),
  VER_PERFIL(ClassroomAction.VER_PERFIL, PrincipalResource.USUARIO),

  CRIAR_TURMA(ClassroomAction.CRIAR_TURMA, TURMA),
  CRIAR_ATIVIDADE(ClassroomAction.CRIAR_ATIVIDADE, TURMA),
  CRIAR_POST(ClassroomAction.CRIAR_POST, TURMA),
  CRIAR_COMENTARIO(ClassroomAction.CRIAR_COMENTARIO, TURMA),
  CRIAR_PROFESSOR(ClassroomAction.CRIAR_PROFESSOR, PrincipalResource.USUARIO),
  CRIAR_ALUNO(ClassroomAction.CRIAR_ALUNO, PrincipalResource.USUARIO),

  EDITAR_TURMA(ClassroomAction.EDITAR_TURMA, TURMA),
  EDITAR_ATIVIDADE(ClassroomAction.EDITAR_ATIVIDADE, ATIVIDADE),
  EDITAR_POST(ClassroomAction.EDITAR_POST, POST),
  EDITAR_COMENTARIO(ClassroomAction.EDITAR_COMENTARIO, COMENTARIO),
  EDITAR_USUARIO(ClassroomAction.EDITAR_USUARIO, PrincipalResource.USUARIO),

  EXCLUIR_TURMA(ClassroomAction.EXCLUIR_TURMA, TURMA),
  EXCLUIR_ATIVIDADE(ClassroomAction.EXCLUIR_ATIVIDADE, ATIVIDADE),
  EXCLUIR_POST(ClassroomAction.EXCLUIR_POST, POST),
  EXCLUIR_COMENTARIO(ClassroomAction.EXCLUIR_COMENTARIO, COMENTARIO),
  EXCLUIR_USUARIO(ClassroomAction.EXCLUIR_USUARIO, PrincipalResource.USUARIO),

  MATRICULAR_ALUNO(ClassroomAction.MATRICULAR_ALUNO, TURMA),
  DESMATRICULAR_ALUNO(ClassroomAction.DESMATRICULAR_ALUNO, TURMA);

  private final Permission permission;

  ClassroomPermission(Action action, ResourceType resourceType) {
    this.permission = new Permission(action, resourceType);
  }

  public boolean isAllowed(User user, Resource resource, Object... context) {
    return AccessResolver.isAllowed(user, permission, resource, context);
  }

  /** Para as ações que não têm um recurso alvo, como criar uma turma. */
  public boolean isAllowed(User user) {
    return isAllowed(user, null);
  }

  public Permission get() {
    return permission;
  }
}
