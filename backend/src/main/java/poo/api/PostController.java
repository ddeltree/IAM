package poo.api;

import io.javalin.Javalin;
import io.javalin.http.Context;
import poo.classroom.*;
import poo.iam.User;

import java.util.*;

public class PostController {

  private static final Map<String, Post> posts = new HashMap<>();

  public static Post get(String id) {
    return posts.get(id);
  }

  public static void register(Javalin app) {
    app.get("/posts", PostController::listar);
    app.get("/posts/{id}", PostController::ver);
    app.post("/posts", PostController::criar);
    app.put("/posts/{id}", PostController::atualizar);
    app.delete("/posts/{id}", PostController::excluir);
  }

  private static void listar(Context ctx) {
    ctx.json(posts.values());
  }

  private static void ver(Context ctx) {
    String id = ctx.pathParam("id");
    Post post = posts.get(id);
    if (post == null) {
      ctx.status(404).result("Post não encontrado");
      return;
    }
    ctx.json(post);
  }

  private static void criar(Context ctx) {
    PostDTO dto = ctx.bodyAsClass(PostDTO.class);
    User autor = UserController.getUser(dto.autorId);
    if (autor == null) {
      ctx.status(404).result("Autor não encontrado");
      return;
    }
    var turma = TurmaController.getTurma(dto.turmaId);
    if (turma == null) {
      ctx.status(404).result("Turma não encontrada");
      return;
    }
    Post post = new Post(dto.titulo, dto.corpo, autor, turma);
    posts.put(post.getId(), post);
    ctx.status(201).json(post);
  }

  private static void atualizar(Context ctx) {
    String id = ctx.pathParam("id");
    Post post = posts.get(id);
    if (post == null) {
      ctx.status(404).result("Post não encontrado");
      return;
    }
    PostDTO dto = ctx.bodyAsClass(PostDTO.class);
    post.setTitulo(dto.titulo);
    post.setCorpo(dto.corpo);
    ctx.status(200).json(post);
  }

  private static void excluir(Context ctx) {
    String id = ctx.pathParam("id");
    if (posts.remove(id) == null) {
      ctx.status(404).result("Post não encontrado");
    } else {
      ctx.status(204);
    }
  }

  public static class PostDTO {
    public String titulo;
    public String corpo;
    public String autorId;
    public String turmaId;
  }
}
