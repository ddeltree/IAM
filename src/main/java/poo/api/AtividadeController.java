package poo.api;

import io.javalin.Javalin;
import io.javalin.http.Context;
import poo.classroom.Atividade;
import poo.classroom.Turma;
import poo.iam.User;

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

  private static void listar(Context ctx) {
    ctx.json(atividades.values());
  }

  private static void ver(Context ctx) {
    String id = ctx.pathParam("id");
    Atividade atividade = atividades.get(id);
    if (atividade == null) {
      ctx.status(404).result("Atividade não encontrada");
      return;
    }
    ctx.json(atividade);
  }

  private static void criar(Context ctx) {
    AtividadeDTO dto = ctx.bodyAsClass(AtividadeDTO.class);
    var turma = TurmaController.getTurma(dto.turmaId);
    Atividade atividade = new Atividade(dto.titulo, dto.corpo, turma);
    atividade.setDataEntrega(dto.dataEntrega);
    atividades.put(atividade.getId(), atividade);
    ctx.status(201).json(atividade);
  }

  private static void atualizar(Context ctx) {
    String id = ctx.pathParam("id");
    Atividade atividade = atividades.get(id);
    if (atividade == null) {
      ctx.status(404).result("Atividade não encontrada");
      return;
    }
    AtividadeDTO dto = ctx.bodyAsClass(AtividadeDTO.class);
    atividade.setTitulo(dto.titulo);
    atividade.setCorpo(dto.corpo);
    atividade.setDataEntrega(dto.dataEntrega);
    ctx.status(200).json(atividade);
  }

  private static void excluir(Context ctx) {
    String id = ctx.pathParam("id");
    if (atividades.remove(id) == null) {
      ctx.status(404).result("Atividade não encontrada");
    } else {
      ctx.status(204);
    }
  }

  public static class AtividadeDTO {
    public String titulo;
    public String corpo;
    public String dataEntrega; // ISO-8601 (ex: "2025-05-15")
    public String turmaId;
  }
}
