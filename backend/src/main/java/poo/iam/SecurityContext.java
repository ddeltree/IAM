package poo.iam;

import java.util.List;

import poo.api.UserController;

import static poo.iam.SystemPermission.*;

public class SecurityContext {
  private static final SecurityContext instance = new SecurityContext(); // Singleton

  private User admin;
  private Group alunos;
  private Group professores;

  private SecurityContext() {
    this.admin = new User("ADMIN");
    var adminPermissions = List.of(
        LISTAR_TURMAS_ADM,
        LISTAR_ATIVIDADES,
        LISTAR_POSTS,
        LISTAR_COMENTARIOS,
        LISTAR_PARTICIPANTES,
        CRIAR_PROFESSOR,
        EDITAR_ATIVIDADE,
        EDITAR_POST,
        EDITAR_COMENTARIO,
        EXCLUIR_ATIVIDADE,
        // EXCLUIR_USUARIO,
        EXCLUIR_POST,
        EXCLUIR_COMENTARIO);
    for (SystemPermission perm : adminPermissions) {
      admin.grantPermission(perm.get());
    }

    this.alunos = new Group("Alunos");
    var alunosPermissions = List.of(
        LISTAR_TURMAS_ALUNO,
        LISTAR_ATIVIDADES,
        LISTAR_POSTS,
        LISTAR_COMENTARIOS,
        LISTAR_PARTICIPANTES,
        CRIAR_POST,
        CRIAR_COMENTARIO,
        EDITAR_POST,
        EDITAR_COMENTARIO,
        EXCLUIR_POST,
        EXCLUIR_COMENTARIO);
    for (SystemPermission perm : alunosPermissions) {
      alunos.grantPermission(perm.get());
    }

    this.professores = new Group("Professores");
    var professoresPermissions = List.of(
        LISTAR_TURMAS_PROFESSOR,
        LISTAR_ATIVIDADES,
        LISTAR_POSTS,
        LISTAR_COMENTARIOS,
        LISTAR_PARTICIPANTES,
        CRIAR_TURMA,
        CRIAR_ATIVIDADE,
        CRIAR_POST,
        CRIAR_COMENTARIO,
        CRIAR_ALUNO,
        EDITAR_TURMA,
        EDITAR_ATIVIDADE,
        EDITAR_POST,
        EDITAR_COMENTARIO,
        EXCLUIR_TURMA,
        EXCLUIR_ATIVIDADE,
        EXCLUIR_POST,
        EXCLUIR_COMENTARIO,
        MATRICULAR_ALUNO,
        DESMATRICULAR_ALUNO);
    for (SystemPermission perm : professoresPermissions) {
      professores.grantPermission(perm.get());
    }
  }

  public static SecurityContext getInstance() {
    return instance;
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
    return user.getGroups().contains(professores);
  }

  public boolean isProfessor(String uid) {
    var user = UserController.getUser(uid);
    return isProfessor(user);
  }

  public boolean isAluno(User user) {
    return user.getGroups().contains(alunos);
  }

  public boolean isAdmin(User user) {
    return user.getId().equals(admin.getId());
  }

  public boolean isAdmin(String uid) {
    return uid.equals(admin.getId());
  }
}
