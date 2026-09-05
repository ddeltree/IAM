package poo.classroom.iam;

import static poo.classroom.iam.ClassroomAction.*;
import static poo.classroom.iam.ClassroomResource.*;

import poo.iam.Action;
import poo.iam.Decisao;
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
  LISTAR_USUARIOS(ClassroomAction.LISTAR_USUARIOS, PrincipalResource.USUARIO, Escopo.GLOBAL),
  VER_PERFIL(ClassroomAction.VER_PERFIL, PrincipalResource.USUARIO),

  CRIAR_TURMA(ClassroomAction.CRIAR_TURMA, TURMA, Escopo.GLOBAL),
  CRIAR_ATIVIDADE(ClassroomAction.CRIAR_ATIVIDADE, TURMA),
  CRIAR_POST(ClassroomAction.CRIAR_POST, TURMA),
  CRIAR_COMENTARIO(ClassroomAction.CRIAR_COMENTARIO, TURMA),
  CRIAR_PROFESSOR(ClassroomAction.CRIAR_PROFESSOR, PrincipalResource.USUARIO, Escopo.GLOBAL),
  CRIAR_ALUNO(ClassroomAction.CRIAR_ALUNO, PrincipalResource.USUARIO, Escopo.GLOBAL),

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

  /**
   * Ações sem alvo (criar uma turma, listar usuários) não fazem sentido num
   * mapa de permissões efetivas sobre um recurso — este escopo é o que as
   * separa.
   */
  public enum Escopo {
    GLOBAL,
    RECURSO,
  }

  private final Permission permission;
  private final Escopo escopo;

  ClassroomPermission(Action action, ResourceType resourceType) {
    this(action, resourceType, Escopo.RECURSO);
  }

  ClassroomPermission(Action action, ResourceType resourceType, Escopo escopo) {
    this.permission = new Permission(action, resourceType);
    this.escopo = escopo;
  }

  public Escopo getEscopo() {
    return escopo;
  }

  public ResourceType getResourceType() {
    return permission.getResourceType();
  }

  public boolean isAllowed(User user, Resource resource, Object... context) {
    return motor().isAllowed(user, permission, resource, context);
  }

  /** Como {@link #isAllowed}, mas dizendo qual cláusula decidiu e por quê. */
  public Decisao avaliar(User user, Resource resource, Object... context) {
    return motor().avaliar(user, permission, resource, context);
  }

  /**
   * O motor desta aplicação. Não é mais um estático do núcleo: é a instância
   * que o {@link SecurityContext} montou com os provedores de atributo daqui.
   */
  private static poo.iam.AuthorizationEngine motor() {
    return SecurityContext.getInstance().iam().motor();
  }

  /** Para as ações que não têm um recurso alvo, como criar uma turma. */
  public boolean isAllowed(User user) {
    return isAllowed(user, null);
  }

  public Permission get() {
    return permission;
  }
}
