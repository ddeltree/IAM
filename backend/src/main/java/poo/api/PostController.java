package poo.api;

import io.javalin.Javalin;
import io.javalin.http.Context;
import poo.api.exceptions.NotFoundException;
import poo.classroom.*;
import poo.iam.User;

import static poo.iam.SystemPermission.*;

import java.util.*;

public class PostController {

  private static final Map<String, Post> posts = new HashMap<>();

  public static Post get(String id) {
    return posts.get(id);
  }

  public static void register(Javalin app) {
    app.get("/turmas/{turmaId}/posts", PostController::listar);
    app.get("/turmas/{turmaId}/posts/{postId}", PostController::ver);
    app.post("/turmas/{turmaId}/posts", PostController::criar);
    app.put("/turmas/{turmaId}/posts/{postId}", PostController::atualizar);
    app.delete("/turmas/{turmaId}/posts/{postId}", PostController::excluir);
  }

  private static void listar(Context ctx) {
    var turma = TurmaController.findTurmaOrThrow(ctx.pathParam("turmaId"));
    if (!Utils.hasPermissionOrThrow(ctx, LISTAR_POSTS, turma))
      return;
    ctx.json(turma.getPosts());
  }

  private static void ver(Context ctx) {
    var post = findPostOrThrow(ctx);
    if (!Utils.hasPermissionOrThrow(ctx, LISTAR_POSTS, post.getTurma()))
      return;
    ctx.json(post);
  }

  private static void criar(Context ctx) {
    var turma = TurmaController.findTurmaOrThrow(ctx.pathParam("turmaId"));
    if (!Utils.hasPermissionOrThrow(ctx, CRIAR_POST, turma))
      return;
    User user = Utils.findAuthUserOrThrow(ctx);
    PostDTO dto = ctx.bodyAsClass(PostDTO.class);
    Post post = new Post(dto.titulo, dto.corpo, user, turma);
    posts.put(post.getId(), post);
    turma.adicionarPost(post);
    ctx.status(201).json(post);
  }

  private static void atualizar(Context ctx) {
    var post = findPostOrThrow(ctx);
    if (!Utils.hasPermissionOrThrow(ctx, EDITAR_POST, post))
      return;
    PostDTO dto = ctx.bodyAsClass(PostDTO.class);
    post.setTitulo(dto.titulo);
    post.setCorpo(dto.corpo);
    ctx.status(200).json(post);
  }

  private static void excluir(Context ctx) {
    var post = findPostOrThrow(ctx);
    if (!Utils.hasPermissionOrThrow(ctx, EXCLUIR_POST, post))
      return;
    posts.remove(post.getId());
    post.getTurma().removerPost(post);
    ComentarioController.removerComentariosDe(post);
    ctx.status(204);
  }

  /**
   * Busca o post da rota conferindo que ele pertence à turma do caminho — sem
   * isso um id de outra turma passaria pela URL sem ser notado.
   */
  private static Post findPostOrThrow(Context ctx) {
    var turma = TurmaController.findTurmaOrThrow(ctx.pathParam("turmaId"));
    var post = findPostOrThrow(ctx.pathParam("postId"));
    if (!post.getTurma().equals(turma))
      throw new NotFoundException("Post não encontrado");
    return post;
  }

  public static Post findPostOrThrow(String id) {
    Post post = posts.get(id);
    if (post == null)
      throw new NotFoundException("Post não encontrado");
    return post;
  }

  /** Remove em cascata os posts de uma turma excluída. */
  static void removerPostsDe(Turma turma) {
    var daTurma = posts.values().stream()
        .filter(p -> p.getTurma().equals(turma))
        .toList();
    for (Post post : daTurma) {
      posts.remove(post.getId());
      ComentarioController.removerComentariosDe(post);
    }
  }

  /** Esvazia o repositório em memória. Usado pelos testes. */
  public static void reset() {
    posts.clear();
  }

  public static class PostDTO {
    public String titulo;
    public String corpo;
  }
}
