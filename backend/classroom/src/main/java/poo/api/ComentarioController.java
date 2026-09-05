package poo.api;

import io.javalin.Javalin;
import io.javalin.http.Context;
import poo.api.exceptions.NotFoundException;
import poo.classroom.Comentario;
import poo.classroom.Publicacao;

import static poo.classroom.iam.ClassroomPermission.*;

import java.util.*;

public class ComentarioController {

  private static final Map<String, Comentario> comentarios = new HashMap<>();

  public static Comentario get(String id) {
    return comentarios.get(id);
  }

  public static void register(Javalin app) {
    app.get("/turmas/{turmaId}/{publicacao}/{pubId}/comentarios", ComentarioController::listar);
    app.get("/turmas/{turmaId}/{publicacao}/{pubId}/comentarios/{id}", ComentarioController::ver);
    app.post("/turmas/{turmaId}/{publicacao}/{pubId}/comentarios", ComentarioController::criar);
    app.put("/turmas/{turmaId}/{publicacao}/{pubId}/comentarios/{id}", ComentarioController::atualizar);
    app.delete("/turmas/{turmaId}/{publicacao}/{pubId}/comentarios/{id}", ComentarioController::excluir);
  }

  private static void listar(Context ctx) {
    Publicacao publicacao = findPublicacaoOrThrow(ctx);
    if (!Utils.hasPermissionOrThrow(ctx, LISTAR_COMENTARIOS, publicacao.getTurma()))
      return;
    ctx.json(publicacao.getComentarios());
  }

  private static void ver(Context ctx) {
    var comentario = findComentarioOrThrow(ctx);
    if (!Utils.hasPermissionOrThrow(ctx, LISTAR_COMENTARIOS, comentario.getPublicacao().getTurma()))
      return;
    ctx.json(comentario);
  }

  private static void criar(Context ctx) {
    var publicacao = findPublicacaoOrThrow(ctx);
    if (!Utils.hasPermissionOrThrow(ctx, CRIAR_COMENTARIO, publicacao.getTurma()))
      return;
    var autor = Utils.findAuthUserOrThrow(ctx);
    ComentarioDTO dto = ctx.bodyAsClass(ComentarioDTO.class);
    var comentario = new Comentario(dto.conteudo, autor, publicacao);
    comentarios.put(comentario.getId(), comentario);
    publicacao.adicionarComentario(comentario);
    ctx.status(201).json(comentario);
  }

  private static void atualizar(Context ctx) {
    var comentario = findComentarioOrThrow(ctx);
    if (!Utils.hasPermissionOrThrow(ctx, EDITAR_COMENTARIO, comentario))
      return;
    ComentarioDTO dto = ctx.bodyAsClass(ComentarioDTO.class);
    comentario.setConteudo(dto.conteudo);
    ctx.status(200).json(comentario);
  }

  private static void excluir(Context ctx) {
    var comentario = findComentarioOrThrow(ctx);
    if (!Utils.hasPermissionOrThrow(ctx, EXCLUIR_COMENTARIO, comentario))
      return;
    comentarios.remove(comentario.getId());
    comentario.getPublicacao().removerComentario(comentario);
    ctx.status(204);
  }

  /**
   * Busca o comentário da rota conferindo que ele pertence à publicação do
   * caminho.
   */
  private static Comentario findComentarioOrThrow(Context ctx) {
    var publicacao = findPublicacaoOrThrow(ctx);
    var comentario = findComentarioOrThrow(ctx.pathParam("id"));
    if (!comentario.getPublicacao().equals(publicacao))
      throw new NotFoundException("Comentario não encontrado");
    return comentario;
  }

  public static Comentario findComentarioOrThrow(String id) {
    var com = comentarios.get(id);
    if (com == null)
      throw new NotFoundException("Comentario não encontrado");
    return com;
  }

  /**
   * Resolve a publicação (post ou atividade) do caminho, conferindo que ela
   * pertence à turma da rota.
   */
  private static Publicacao findPublicacaoOrThrow(Context ctx) {
    var turma = TurmaController.findTurmaOrThrow(ctx.pathParam("turmaId"));
    var tipoPublicacao = ctx.pathParam("publicacao"); // 'atividades' | 'posts'
    var pubId = ctx.pathParam("pubId");
    Publicacao publicacao;
    if (tipoPublicacao.equals("atividades"))
      publicacao = AtividadeController.findAtividadeOrThrow(pubId);
    else if (tipoPublicacao.equals("posts"))
      publicacao = PostController.findPostOrThrow(pubId);
    else
      throw new NotFoundException("Tipo de publicação (posts | atividades) inválido: " + tipoPublicacao);
    if (!publicacao.getTurma().equals(turma))
      throw new NotFoundException("Publicação não encontrada nesta turma");
    return publicacao;
  }

  /** Remove em cascata os comentários de uma publicação excluída. */
  static void removerComentariosDe(Publicacao publicacao) {
    var daPublicacao = comentarios.values().stream()
        .filter(c -> c.getPublicacao().equals(publicacao))
        .toList();
    for (Comentario comentario : daPublicacao) {
      comentarios.remove(comentario.getId());
      publicacao.removerComentario(comentario);
    }
  }

  /** Esvazia o repositório em memória. Usado pelos testes. */
  public static void reset() {
    comentarios.clear();
  }

  public static class ComentarioDTO {
    public String conteudo;
  }
}
