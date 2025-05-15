package poo.api;

import io.javalin.Javalin;
import io.javalin.http.Context;
import poo.iam.User;

import java.util.*;

public class UserController {

  private static final Map<String, User> usuarios = new HashMap<>();

  public static User getUser(String id) {
    return usuarios.get(id);
  }

  public static void register(Javalin app) {
    app.get("/usuarios", UserController::listar);
    app.get("/usuarios/{id}", UserController::ver);
    app.post("/usuarios", UserController::criar);
    app.put("/usuarios/{id}", UserController::atualizar);
    app.delete("/usuarios/{id}", UserController::excluir);
  }

  private static void listar(Context ctx) {
    ctx.json(usuarios.values().stream().map(UserController::toDTO).toList());
  }

  private static void ver(Context ctx) {
    String id = ctx.pathParam("id");
    User user = usuarios.get(id);
    if (user == null) {
      ctx.status(404).result("Usuário não encontrado");
      return;
    }
    ctx.json(toDTO(user));
  }

  private static void criar(Context ctx) {
    UserDTO dto = ctx.bodyAsClass(UserDTO.class);
    User user = new User();
    user.setName(dto.name);
    usuarios.put(user.getId(), user);
    ctx.status(201).json(toDTO(user));
  }

  private static void atualizar(Context ctx) {
    String id = ctx.pathParam("id");
    User user = usuarios.get(id);
    if (user == null) {
      ctx.status(404).result("Usuário não encontrado");
      return;
    }
    UserDTO dto = ctx.bodyAsClass(UserDTO.class);
    user.setName(dto.name);
    ctx.status(200).json(toDTO(user));
  }

  private static void excluir(Context ctx) {
    String id = ctx.pathParam("id");
    if (usuarios.remove(id) == null) {
      ctx.status(404).result("Usuário não encontrado");
    } else {
      ctx.status(204);
    }
  }

  // DTO para serialização e entrada de dados
  public static class UserDTO {
    public String id;
    public String name;
    public String tipo;
  }

  private static UserDTO toDTO(User user) {
    UserDTO dto = new UserDTO();
    dto.id = user.getId();
    dto.name = user.getName();
    return dto;
  }
}
