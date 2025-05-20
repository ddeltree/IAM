package poo.iam;

import poo.api.Participante;
import poo.api.Utils;
import poo.classroom.Atividade;
import poo.classroom.Comentario;
import poo.classroom.Post;
import poo.classroom.Publicacao;
import poo.classroom.Turma;
import poo.iam.resources.Resource;
import poo.iam.resources.ResourceTypes;

public enum SystemPermission {
  LISTAR_TURMAS_ADM(Action.LISTAR_TURMAS_ADM, ResourceTypes.TURMA),
  LISTAR_TURMAS_PROFESSOR(Action.LISTAR_TURMAS_PROFESSOR, ResourceTypes.TURMA, (user, resource, __) -> {
    var turma = (Turma) resource;
    return turma.getProfessorResponsavel().equals(user);
  }),
  LISTAR_TURMAS_ALUNO(Action.LISTAR_TURMAS_ALUNO, ResourceTypes.TURMA, (user, resource, ___) -> {
    var turma = (Turma) resource;
    return turma.temAluno(user.getId());
  }),
  VER_TURMA(Action.VER_TURMA, ResourceTypes.TURMA, (user, resource, __) -> {
    return isAdminOrParticipante(user, resource);
  }),

  LISTAR_ATIVIDADES(Action.LISTAR_ATIVIDADES, ResourceTypes.ATIVIDADE, (user, turma, __) -> {
    return isAdminOrParticipante(user, turma);
  }),
  LISTAR_POSTS(Action.LISTAR_POSTS, ResourceTypes.POST, (user, turma, __) -> {
    return isAdminOrParticipante(user, turma);
  }),
  LISTAR_COMENTARIOS(Action.LISTAR_COMENTARIOS, ResourceTypes.COMENTARIO, (user, turma, __) -> {
    return isAdminOrParticipante(user, turma);
  }),

  LISTAR_PARTICIPANTES(Action.LISTAR_PARTICIPANTES, ResourceTypes.USUARIO, (user, resource, __) -> {
    return isAdminOrParticipante(user, resource);
  }),
  LISTAR_USUARIOS(Action.LISTAR_USUARIOS, ResourceTypes.USUARIO),
  VER_PERFIL(Action.VER_PERFIL, ResourceTypes.USUARIO, (user, resource, __) -> {
    User profileOwner = (User) resource;
    return user.equals(profileOwner);
  }),

  CRIAR_TURMA(Action.CRIAR_TURMA, ResourceTypes.TURMA),
  CRIAR_ATIVIDADE(Action.CRIAR_ATIVIDADE, ResourceTypes.ATIVIDADE, (user, turma, __) -> {
    return isProfessorResponsavel(user, turma);
  }),
  CRIAR_POST(Action.CRIAR_POST, ResourceTypes.POST, (user, turma, __) -> {
    return isParticipante(user, turma);
  }),
  CRIAR_COMENTARIO(Action.CRIAR_COMENTARIO, ResourceTypes.COMENTARIO, (user, turma, __) -> {
    return isParticipante(user, turma);
  }),
  CRIAR_PROFESSOR(Action.CRIAR_PROFESSOR, ResourceTypes.USUARIO),
  CRIAR_ALUNO(Action.CRIAR_ALUNO, ResourceTypes.USUARIO),

  EDITAR_TURMA(Action.EDITAR_TURMA, ResourceTypes.TURMA, (user, turma, __) -> {
    return isProfessorResponsavel(user, turma);
  }),
  EDITAR_ATIVIDADE(Action.EDITAR_ATIVIDADE, ResourceTypes.ATIVIDADE, (user, resource, __) -> {
    var atividade = (Atividade) resource;
    var isAdmin = Utils.isAdmin(user.getId());
    return isAdmin || isProfessorResponsavel(user, atividade.getTurma());
  }),
  EDITAR_POST(Action.EDITAR_POST, ResourceTypes.POST, (user, resource, __) -> {
    var post = (Post) resource;
    return Utils.isAdmin(user.getId()) || user.equals(post.getAutor());
  }),
  EDITAR_COMENTARIO(Action.EDITAR_COMENTARIO, ResourceTypes.COMENTARIO, (user, resource, __) -> {
    var comentario = (Comentario) resource;
    return Utils.isAdmin(user.getId()) || user.equals(comentario.getAutor());
  }),
  EDITAR_USUARIO(Action.EDITAR_USUARIO, ResourceTypes.USUARIO, (user, resource, __) -> {
    User profileOwner = (User) resource;
    return user.equals(profileOwner);
  }),

  EXCLUIR_TURMA(Action.EXCLUIR_TURMA, ResourceTypes.TURMA, (user, resource, __) -> {
    var turma = (Turma) resource;
    return isProfessorResponsavel(user, turma);
  }),
  EXCLUIR_ATIVIDADE(Action.EXCLUIR_ATIVIDADE, ResourceTypes.ATIVIDADE, (user, resource, __) -> {
    return Utils.isAdmin(user) || isAutor(user, resource);
  }),
  EXCLUIR_POST(Action.EXCLUIR_POST, ResourceTypes.POST, (user, resource, __) -> {
    var post = (Post) resource;
    var isProf = post.getTurma().getProfessorResponsavel().equals(user);
    return Utils.isAdmin(user) || isProf || isAutor(user, resource);
  }),
  EXCLUIR_COMENTARIO(Action.EXCLUIR_COMENTARIO, ResourceTypes.COMENTARIO, (user, resource, __) -> {
    var comentario = (Comentario) resource;
    var isProf = comentario.getPost().getTurma().getProfessorResponsavel().equals(user);
    return Utils.isAdmin(user) || isProf || isAutor(user, comentario);
  }),
  // apenas o ADMIN e o próprio USUÁRIO
  EXCLUIR_USUARIO(Action.EXCLUIR_USUARIO, ResourceTypes.USUARIO, (user, resource, __) -> {
    User profileOwner = (User) resource;
    return Utils.isAdmin(user) || user.equals(profileOwner);
  }),

  MATRICULAR_ALUNO(Action.MATRICULAR_ALUNO, ResourceTypes.USUARIO),
  DESMATRICULAR_ALUNO(Action.DESMATRICULAR_ALUNO, ResourceTypes.USUARIO, (user, turma, __) -> {
    return isProfessorResponsavel(user, turma);
  });

  private final Permission permission;
  private final PermissionCondition condition;
  private static PermissionCondition DEFAULT_CONDITION;

  SystemPermission(Action action, ResourceTypes resource) {
    this(action, resource, defaultCondition()); // sem regra contextual
  }

  SystemPermission(Action action, ResourceTypes resource, PermissionCondition condition) {
    this.permission = new Permission(action, resource);
    this.condition = condition;
  }

  public boolean isAllowed(User user, Resource resource, Object... args) {
    if (!inheritsPermission(user, permission))
      return false;
    return condition.test(user, resource, args);
  }

  public boolean isAllowed(User user) {
    if (condition != DEFAULT_CONDITION)
      throw new UnsupportedOperationException("A permissão " + permission + " exige uma condição");
    return isAllowed(user, null, new Object[0]);
  }

  public Permission get() {
    return permission;
  }

  private static boolean inheritsPermission(User user, Permission permission) {
    if (user.hasInlinePermission(permission))
      return true;
    for (Group group : user.getGroups()) {
      if (group.hasPermission(permission))
        return true;
    }
    return false;
  }

  private static boolean isAdminOrParticipante(User user, Resource turma) {
    var isAdmin = Utils.isAdmin(user.getId());
    return isAdmin || isParticipante(user, turma);
  }

  private static boolean isParticipante(User user, Resource turma) {
    var _turma = (Turma) turma;
    return Participante.isParticipante(user.getId(), _turma.getId());
  }

  private static boolean isProfessorResponsavel(User user, Resource turma) {
    var _turma = (Turma) turma;
    return _turma.getProfessorResponsavel().equals(user);
  }

  private static boolean isAutor(User user, Resource resource) {
    if (resource instanceof Publicacao)
      return user.equals(((Publicacao) resource).getAutor());
    else if (resource instanceof Comentario)
      return user.equals(((Comentario) resource).getAutor());
    else
      return false;
  }

  private static PermissionCondition defaultCondition() {
    if (DEFAULT_CONDITION == null) {
      DEFAULT_CONDITION = (u, r, o) -> true;
    }
    return DEFAULT_CONDITION;
  }
}

/*
 * MATRICULAR é inserir o aluno na turma
 * AUTOR é o professor ou aluno que criou a dada publicação ou comentário
 * PARTICIPANTE é o professor autor ou o aluno matriculado em uma turma
 * 
 */