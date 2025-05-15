package poo.api;

import io.javalin.Javalin;
import io.javalin.http.Context;
import poo.classroom.Comentario;

import java.util.*;

public class ComentarioController {

  private static final Map<String, Comentario> comentarios = new HashMap<>();

  public static void register(Javalin app) {
    app.get("/comentarios", ComentarioController::listar);
    app.get("/comentarios/{id}", ComentarioController::ver);
    app.post("/comentarios", ComentarioController::criar);
    app.put("/comentarios/{id}", ComentarioController::atualizar);
    app.delete("/comentarios/{id}", ComentarioController::excluir);
  }

  private static void listar(Context ctx) {
    ctx.json(comentarios.values());
  }

  private static void ver(Context ctx) {
    String id = ctx.pathParam("id");
    Comentario comentario = comentarios.get(id);
    if (comentario == null) {
      ctx.status(404).result("Comentário não encontrado");
      return;
    }
    ctx.json(comentario);
  }

  private static void criar(Context ctx) {
    ComentarioDTO dto = ctx.bodyAsClass(ComentarioDTO.class);
    var post = PostController.get(dto.postId);
    var autor = UserController.getUser(dto.autorId);
    Comentario comentario = new Comentario(dto.conteudo, autor, post);
    comentarios.put(comentario.getId(), comentario);
    ctx.status(201).json(comentario);
  }

  private static void atualizar(Context ctx) {
    String id = ctx.pathParam("id");
    Comentario comentario = comentarios.get(id);
    if (comentario == null) {
      ctx.status(404).result("Comentário não encontrado");
      return;
    }
    ComentarioDTO dto = ctx.bodyAsClass(ComentarioDTO.class);
    comentario.setConteudo(dto.conteudo);
    ctx.status(200).json(comentario);
  }

  private static void excluir(Context ctx) {
    String id = ctx.pathParam("id");
    if (comentarios.remove(id) == null) {
      ctx.status(404).result("Comentário não encontrado");
    } else {
      ctx.status(204);
    }
  }

  public static class ComentarioDTO {
    public String conteudo;
    public String autorId;
    public String postId;
  }
}
