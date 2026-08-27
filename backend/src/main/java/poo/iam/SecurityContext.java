package poo.iam;

import java.util.List;

import poo.api.UserController;

import static poo.iam.SystemPermission.*;

public class SecurityContext {
  private static final List<SystemPermission> PERMISSOES_ADMIN = List.of(
      LISTAR_TURMAS_ADM,
      LISTAR_ATIVIDADES,
      LISTAR_POSTS,
      LISTAR_COMENTARIOS,
      LISTAR_PARTICIPANTES,
      LISTAR_USUARIOS,
      CRIAR_PROFESSOR,
      VER_TURMA,
      EDITAR_ATIVIDADE,
      EDITAR_POST,
      EDITAR_COMENTARIO,
      EXCLUIR_ATIVIDADE,
      EXCLUIR_USUARIO,
      EXCLUIR_POST,
      EXCLUIR_COMENTARIO);

  private static final List<SystemPermission> PERMISSOES_ALUNOS = List.of(
      LISTAR_TURMAS_ALUNO,
      LISTAR_ATIVIDADES,
      LISTAR_POSTS,
      LISTAR_COMENTARIOS,
      LISTAR_PARTICIPANTES,
      VER_TURMA,
      VER_PERFIL,
      CRIAR_POST,
      CRIAR_COMENTARIO,
      EDITAR_POST,
      EDITAR_COMENTARIO,
      EDITAR_USUARIO,
      EXCLUIR_USUARIO,
      EXCLUIR_POST,
      EXCLUIR_COMENTARIO);

  private static final List<SystemPermission> PERMISSOES_PROFESSORES = List.of(
      LISTAR_TURMAS_PROFESSOR,
      LISTAR_ATIVIDADES,
      LISTAR_POSTS,
      LISTAR_COMENTARIOS,
      LISTAR_PARTICIPANTES,
      VER_PERFIL,
      VER_TURMA,
      CRIAR_TURMA,
      CRIAR_ATIVIDADE,
      CRIAR_POST,
      CRIAR_COMENTARIO,
      CRIAR_ALUNO,
      EDITAR_TURMA,
      EDITAR_ATIVIDADE,
      EDITAR_POST,
      EDITAR_USUARIO,
      EDITAR_COMENTARIO,
      EXCLUIR_TURMA,
      EXCLUIR_ATIVIDADE,
      EXCLUIR_POST,
      EXCLUIR_COMENTARIO,
      EXCLUIR_USUARIO,
      MATRICULAR_ALUNO,
      DESMATRICULAR_ALUNO);

  // declarado depois das listas acima: a construção do singleton depende delas
  private static final SecurityContext instance = new SecurityContext(); // Singleton

  private final User admin;
  private final Group alunos;
  private final Group professores;

  private SecurityContext() {
    this.admin = new User("ADMIN");
    this.alunos = new Group("Alunos");
    this.professores = new Group("Professores");
    configurarPermissoesPadrao();
  }

  public static SecurityContext getInstance() {
    return instance;
  }

  /**
   * Devolve o contexto ao estado inicial: grupos vazios e permissões padrão.
   * Usado pelos testes, que compartilham o mesmo singleton entre cenários.
   */
  public void reset() {
    admin.clearPermissions();
    alunos.clearPermissions();
    alunos.clearUsers();
    professores.clearPermissions();
    professores.clearUsers();
    configurarPermissoesPadrao();
  }

  private void configurarPermissoesPadrao() {
    for (SystemPermission perm : PERMISSOES_ADMIN) {
      admin.grantPermission(perm.get());
    }
    for (SystemPermission perm : PERMISSOES_ALUNOS) {
      alunos.grantPermission(perm.get());
    }
    for (SystemPermission perm : PERMISSOES_PROFESSORES) {
      professores.grantPermission(perm.get());
    }
  }

  public User getAdmin() {
    return admin;
  }

  public Group getAlunos() {
    return alunos;
  }

  public Group getProfessores() {
    return professores;
  }

  public boolean isProfessor(User user) {
    return user != null && user.getGroups().contains(professores);
  }

  public boolean isProfessor(String uid) {
    var user = UserController.getUser(uid);
    return isProfessor(user);
  }

  public boolean isAluno(User user) {
    return user != null && user.getGroups().contains(alunos);
  }

  public boolean isAdmin(User user) {
    return user != null && user.getId().equals(admin.getId());
  }

  public boolean isAdmin(String uid) {
    return uid.equals(admin.getId());
  }
}
