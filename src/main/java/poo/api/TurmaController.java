package poo.api;

import io.javalin.Javalin;
import io.javalin.http.Context;
import poo.classroom.Turma;
import poo.iam.User;

import java.util.*;

public class TurmaController {

  private static final Map<String, Turma> turmas = new HashMap<>();

  public static Turma getTurma(String id) {
    return turmas.get(id);
  }

  public static void register(Javalin app) {
    app.get("/turmas", TurmaController::listar);
    app.get("/turmas/{id}", TurmaController::ver);
    app.post("/turmas", TurmaController::criar);
    app.put("/turmas/{id}", TurmaController::atualizar);
    app.delete("/turmas/{id}", TurmaController::excluir);
  }

  private static void listar(Context ctx) {
    ctx.json(turmas.values());
  }

  private static void ver(Context ctx) {
    String id = ctx.pathParam("id");
    Turma turma = turmas.get(id);
    if (turma == null) {
      ctx.status(404).result("Turma não encontrada");
      return;
    }
    ctx.json(turma);
  }

  private static void criar(Context ctx) {
    TurmaDTO dto = ctx.bodyAsClass(TurmaDTO.class);
    User professor = UserController.getUser(dto.professorId);
    if (professor == null) {
      ctx.status(404).result("Professor não encontrado");
      return;
    }
    Turma turma = new Turma(dto.nome, professor);
    turmas.put(turma.getId(), turma);
    ctx.status(201).json(turma);
  }

  private static void atualizar(Context ctx) {
    String id = ctx.pathParam("id");
    Turma turma = turmas.get(id);
    if (turma == null) {
      ctx.status(404).result("Turma não encontrada");
      return;
    }
    TurmaDTO dto = ctx.bodyAsClass(TurmaDTO.class);
    turma.setNome(dto.nome);
    ctx.status(200).json(turma);
  }

  private static void excluir(Context ctx) {
    String id = ctx.pathParam("id");
    if (turmas.remove(id) == null) {
      ctx.status(404).result("Turma não encontrada");
    } else {
      ctx.status(204);
    }
  }

  // DTO para simplificar entrada de dados
  public static class TurmaDTO {
    public String nome;
    public String professorId;
  }
}
