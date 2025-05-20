package poo.api;

import io.javalin.Javalin;
import io.javalin.http.Context;
import poo.api.exceptions.ForbiddenException;
import poo.iam.MembershipManager;
import poo.iam.SecurityContext;
import static poo.iam.SystemPermission.*;
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
    app.put("/usuarios/{id}", UserController::atualizarNome);
    app.delete("/usuarios/{id}", UserController::excluirUsuario);
  }

  private static void listarUsuarios(Context ctx) {
    if (!Utils.hasPermissionOrThrow(ctx, LISTAR_USUARIOS))
      return;
    ctx.json(usuarios.values().stream().map(UserController::toDTO).toList());
  }

  private static void verPerfil(Context ctx) {
    var authUser = Utils.findAuthUserOrThrow(ctx);
    var profileOwner = usuarios.get(ctx.pathParam("id"));
    if (!VER_PERFIL.isAllowed(authUser, profileOwner))
      throw new ForbiddenException();
    ctx.json(toDTO(profileOwner));
  }

  private static void novoUsuario(Context ctx) {
    UserDTO dto = ctx.bodyAsClass(UserDTO.class);
    switch (dto.tipo) {
      case 0:
        if (!Utils.hasPermissionOrThrow(ctx, CRIAR_ALUNO)) {
          return;
        }
        break;
      case 1:
        if (!Utils.hasPermissionOrThrow(ctx, CRIAR_PROFESSOR))
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

  private static void atualizarNome(Context ctx) {
    var targetUser = getUser(ctx.pathParam("id"));
    if (!Utils.hasPermissionOrThrow(ctx, EDITAR_USUARIO, targetUser))
      return;
    UserDTO dto = ctx.bodyAsClass(UserDTO.class);
    targetUser.setName(dto.name);
    ctx.status(200).json(toDTO(targetUser));
  }

  private static void excluirUsuario(Context ctx) {
    var targetUser = getUser(ctx.pathParam("id"));
    if (!Utils.hasPermissionOrThrow(ctx, EXCLUIR_USUARIO, targetUser))
      return;
    usuarios.remove(targetUser.getId());
    ctx.status(204);
  }

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
