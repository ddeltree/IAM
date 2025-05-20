package poo.api;

import static poo.iam.SystemPermission.*;

import io.javalin.Javalin;
import io.javalin.http.Context;
import poo.iam.SecurityContext;

public class ParticipantesController {
  public static void register(Javalin app) {
    app.get("/turmas/{turmaId}/participantes", ParticipantesController::listarParticipantes);
    app.post("/turmas/{turmaId}/participantes", ParticipantesController::adicionarParticipante);
    app.delete("/turmas/{turmaId}/participantes/{uid}", ParticipantesController::removerParticipante);
  }

  private static void listarParticipantes(Context ctx) {
    var turma = TurmaController.findTurmaOrThrow(ctx.pathParam("turmaId"));
    if (!Utils.hasPermissionOrThrow(ctx, LISTAR_PARTICIPANTES, turma))
      return;
    var participantes = turma.getParticipantes();
    ctx.json(participantes.stream().map(u -> new Participante(u, turma)).toList());
  }

  private static void adicionarParticipante(Context ctx) {
    var turma = TurmaController.findTurmaOrThrow(ctx.pathParam("turmaId"));
    if (!Utils.hasPermissionOrThrow(ctx, MATRICULAR_ALUNO, turma))
      return;
    UserDTO dto = ctx.bodyAsClass(UserDTO.class);
    var aluno = Utils.findUserOrThrow(dto.uid, "Aluno não encontrado");
    var prof = Utils.findAuthUserOrThrow(ctx);
    var auth = SecurityContext.getInstance();
    if (!auth.isAluno(aluno)) {
      ctx.status(400).result("O usuário não é um aluno.");
      return;
    }
    if (!turma.temAluno(aluno)) {
      turma.adicionarAluno(aluno);
    }
    ctx.status(201).json(new Participante(prof, turma));
  }

  private static void removerParticipante(Context ctx) {
    var turma = TurmaController.findTurmaOrThrow(ctx.pathParam("turmaId"));
    if (!Utils.hasPermissionOrThrow(ctx, DESMATRICULAR_ALUNO, turma))
      return;
    var uid = ctx.pathParam("uid");
    var aluno = Utils.findUserOrThrow(uid, "Usuário não encontrado");
    var prof = Utils.findAuthUserOrThrow(ctx);
    var isParticipante = Participante.isParticipante(aluno, turma);
    if (!isParticipante) {
      ctx.status(404).result("Aluno não encontrado na turma");
      return;
    } else if (prof.equals(aluno)) {
      ctx.status(400).result("Não é possivel remover o autor da turma");
      return;
    }
    turma.removerAluno(aluno);
    ctx.status(204);
  }

  public static class UserDTO {
    public String uid;
  }
}
