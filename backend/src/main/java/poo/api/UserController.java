package poo.api;

import io.javalin.Javalin;
import io.javalin.http.Context;
import poo.iam.MembershipManager;
import poo.iam.SecurityContext;
import poo.iam.SystemPermission;
import poo.iam.User;

import java.util.*;

public class UserController {

  private static final Map<String, User> usuarios = new HashMap<>();

  public static User getUser(String id) {
    return usuarios.get(id);
  }

  public static void register(Javalin app) {
    app.get("/usuarios", UserController::listarUsuarios);
    app.get("/usuarios/{id}", UserController::verPerfil);
    app.post("/usuarios", UserController::novoUsuario);
    // app.put("/usuarios/{id}", UserController::atualizar);
    // app.delete("/usuarios/{id}", UserController::excluir);
  }

  /** apenas o ADMIN pode listar todos os usuarios */
  private static void listarUsuarios(Context ctx) {
    if (!Utils.hasPermissionOr403(SystemPermission.LISTAR_USUARIOS, ctx))
      return;
    ctx.json(usuarios.values().stream().map(UserController::toDTO).toList());
  }

  /** apenas o próprio USUÁRIO pode ver seu perfil */
  private static void verPerfil(Context ctx) {
    if (!Utils.hasPermissionOr403(SystemPermission.VER_PERFIL, ctx))
      return;
    var user = Utils.findUserOr404(ctx);
    ctx.json(toDTO(user));
  }

  /**
   * Apenas o ADMIN pode criar professores (tipo 1);
   * apenas o PROFESSOR pode criar alunos (tipo 0)
   */
  private static void novoUsuario(Context ctx) {
    UserDTO dto = ctx.bodyAsClass(UserDTO.class);
    switch (dto.tipo) {
      case 0:
        if (!Utils.hasPermissionOr403(SystemPermission.CRIAR_ALUNO, ctx))
          return;
        break;
      case 1:
        if (!Utils.hasPermissionOr403(SystemPermission.CRIAR_PROFESSOR, ctx))
          return;
        break;
      default:
        ctx.status(400).result("Tipo inválido");
        return;
    }
    var auth = SecurityContext.getInstance();
    User novoUsuario = new User(dto.name);
    MembershipManager.link(novoUsuario, dto.tipo == 1 ? auth.getProfessores() : auth.getAlunos());
    usuarios.put(novoUsuario.getId(), novoUsuario);
    ctx.status(201).json(toDTO(novoUsuario));
  }

  // private static void atualizar(Context ctx) {
  // var user = getUser(ctx);
  // UserDTO dto = ctx.bodyAsClass(UserDTO.class);
  // user.setName(dto.name);
  // ctx.status(200).json(toDTO(user));
  // }

  // private static void excluir(Context ctx) {
  // var user = getUser(ctx);
  // Utils.isAdmin(ctx.)
  // usuarios.remove(user.getId());
  // ctx.status(204);
  // }

  // DTO para serialização e entrada de dados
  public static class UserDTO {
    public String id;
    public String name;
    public int tipo = 0; // 0 = aluno, 1 = professor
    public String turmaId;
    public String autorId;
  }

  private static UserDTO toDTO(User user) {
    var auth = SecurityContext.getInstance();
    UserDTO dto = new UserDTO();
    dto.id = user.getId();
    dto.name = user.getName();
    dto.tipo = auth.isProfessor(user) ? 1 : auth.isAluno(user) ? 0 : -1;
    return dto;
  }
}
