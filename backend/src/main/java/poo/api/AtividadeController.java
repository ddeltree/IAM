package poo.api;

import io.javalin.Javalin;
import io.javalin.http.Context;
import poo.api.exceptions.NotFoundException;
import poo.classroom.Atividade;
import poo.classroom.Turma;

import static poo.classroom.iam.ClassroomPermission.*;

import java.util.*;

public class AtividadeController {

  private static final Map<String, Atividade> atividades = new HashMap<>();

  public static void register(Javalin app) {
    app.get("/atividades", AtividadeController::listar);
    app.get("/atividades/{id}", AtividadeController::ver);
    app.post("/atividades", AtividadeController::criar);
    app.put("/atividades/{id}", AtividadeController::atualizar);
    app.delete("/atividades/{id}", AtividadeController::excluir);
  }

  /**
   * Lista as atividades que o usuário autenticado pode ver. Aceita o filtro
   * opcional {@code ?turmaId=} para restringir a uma turma.
   */
  private static void listar(Context ctx) {
    var user = Utils.findAuthUserOrThrow(ctx);
    var turmaId = ctx.queryParam("turmaId");
    List<Atividade> result = new ArrayList<>();
    for (Atividade atividade : atividades.values()) {
      if (turmaId != null && !atividade.getTurma().getId().equals(turmaId))
        continue;
      if (!LISTAR_ATIVIDADES.isAllowed(user, atividade.getTurma()))
        continue;
      result.add(atividade);
    }
    ctx.json(result);
  }

  private static void ver(Context ctx) {
    var atividade = findAtividadeOrThrow(ctx);
    if (!Utils.hasPermissionOrThrow(ctx, LISTAR_ATIVIDADES, atividade.getTurma()))
      return;
    ctx.json(atividade);
  }

  private static void criar(Context ctx) {
    AtividadeDTO dto = ctx.bodyAsClass(AtividadeDTO.class);
    if (dto.turmaId == null)
      throw new NotFoundException("Turma não encontrada");
    var turma = TurmaController.findTurmaOrThrow(dto.turmaId);
    if (!Utils.hasPermissionOrThrow(ctx, CRIAR_ATIVIDADE, turma))
      return;
    Atividade atividade = new Atividade(dto.titulo, dto.corpo, turma);
    atividade.setDataEntrega(dto.dataEntrega);
    atividades.put(atividade.getId(), atividade);
    ctx.status(201).json(atividade);
  }

  private static void atualizar(Context ctx) {
    var atividade = findAtividadeOrThrow(ctx);
    if (!Utils.hasPermissionOrThrow(ctx, EDITAR_ATIVIDADE, atividade))
      return;
    AtividadeDTO dto = ctx.bodyAsClass(AtividadeDTO.class);
    atividade.setTitulo(dto.titulo);
    atividade.setCorpo(dto.corpo);
    atividade.setDataEntrega(dto.dataEntrega);
    ctx.status(200).json(atividade);
  }

  private static void excluir(Context ctx) {
    var atividade = findAtividadeOrThrow(ctx);
    if (!Utils.hasPermissionOrThrow(ctx, EXCLUIR_ATIVIDADE, atividade))
      return;
    atividades.remove(atividade.getId());
    ComentarioController.removerComentariosDe(atividade);
    ctx.status(204);
  }

  private static Atividade findAtividadeOrThrow(Context ctx) {
    var id = ctx.pathParam("id");
    return findAtividadeOrThrow(id);
  }

  public static Atividade findAtividadeOrThrow(String id) {
    var atv = atividades.get(id);
    if (atv == null)
      throw new NotFoundException("Atividade não encontrada");
    return atv;
  }

  /** Remove em cascata as atividades de uma turma excluída. */
  static void removerAtividadesDe(Turma turma) {
    var daTurma = atividades.values().stream()
        .filter(a -> a.getTurma().equals(turma))
        .toList();
    for (Atividade atividade : daTurma) {
      atividades.remove(atividade.getId());
      ComentarioController.removerComentariosDe(atividade);
    }
  }

  /** Esvazia o repositório em memória. Usado pelos testes. */
  public static void reset() {
    atividades.clear();
  }

  public static class AtividadeDTO {
    public String titulo;
    public String corpo;
    public String dataEntrega; // ISO-8601 (ex: "2025-05-15")
    public String turmaId;
  }
}
