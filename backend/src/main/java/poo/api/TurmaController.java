package poo.api;

import io.javalin.Javalin;
import io.javalin.http.Context;
import poo.classroom.Turma;
import poo.iam.SecurityContext;
import poo.iam.SystemPermission;
import poo.iam.User;

import java.util.*;
import java.util.stream.Collectors;

public class TurmaController {

  private static final Map<String, Turma> turmas = new HashMap<>();

  public static Turma getTurma(String id) {
    return turmas.get(id);
  }

  public static Turma getTurma(Context ctx) {
    String id = ctx.pathParam("id");
    Turma turma = turmas.get(id);
    if (turma == null) {
      ctx.status(404).result("Turma nao encontrada");
      return null;
    }
    return turma;
  }

  public static void register(Javalin app) {
    app.get("/turmas", TurmaController::listarTurmas);
    app.get("/turmas/{id}", TurmaController::verTurma);
    app.get("/turmas/{id}/participantes", TurmaController::listarParticipantes);

    app.post("/turmas", TurmaController::criarTurma);

    app.put("/turmas/{id}", TurmaController::atualizarTurma);

    app.delete("/turmas/{id}", TurmaController::excluirTurma);
  }

  private static void listarTurmas(Context ctx) {
    var uid = ctx.cookie("uid");
    List<Turma> result = new ArrayList<>();
    var turmas$ = turmas.values().stream();
    var isAdmin = Utils.hasPermissionOrThrow(SystemPermission.LISTAR_TURMAS_ADM, ctx);
    var isProfessor = Utils.hasPermissionOrThrow(SystemPermission.LISTAR_TURMAS_PROFESSOR, ctx);
    var isAluno = Utils.hasPermissionOrThrow(SystemPermission.LISTAR_TURMAS_ALUNO, ctx);
    if (isAdmin) {
      result = turmas$.toList();
    } else if (isProfessor) {
      result = turmas$
          .filter(turma -> turma.getProfessorResponsavel().getId().equals(uid)).collect(Collectors.toList());
    } else if (isAluno) {
      result = turmas$.filter(turma -> turma.temAluno(uid))
          .collect(Collectors.toList());
    }
    ctx.json(result);
  }

  private static void listarParticipantes(Context ctx) {
    if (!Utils.hasPermissionOrThrow(SystemPermission.LISTAR_PARTICIPANTES, ctx))
      return;
    var turma = turmas.get(ctx.pathParam("id"));
    var participantes = turma.getParticipantes();
    ctx.json(participantes.stream().map((User user) -> new Participante(user, turma)).toList());
  }

  private static void verTurma(Context ctx) {
    var isAdmin = Utils.hasPermissionOrThrow(SystemPermission.LISTAR_TURMAS_ADM, ctx);
    var uid = ctx.cookie("uid");
    var turmaId = ctx.pathParam("id");
    var isParticipante = Participante.isParticipante(uid, turmaId);
    if (!isAdmin && !isParticipante)
      return;
    Turma turma = turmas.get(turmaId);
    if (turma == null) {
      ctx.status(404).result("Turma não encontrada");
      return;
    }
    ctx.json(turma);
  }

  private static void criarTurma(Context ctx) {
    if (!Utils.hasPermissionOrThrow(SystemPermission.CRIAR_TURMA, ctx))
      return;
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

  private static void atualizarTurma(Context ctx) {
    if (!Utils.hasPermissionOrThrow(SystemPermission.EDITAR_TURMA, ctx))
      return;
    var idTurma = ctx.pathParam("id");
    Turma turma = turmas.get(idTurma);
    if (turma == null) {
      ctx.status(404).result("Turma não encontrada");
      return;
    }
    TurmaDTO dto = ctx.bodyAsClass(TurmaDTO.class);
    turma.setNome(dto.nome);
    ctx.status(200).json(turma);
  }

  private static void excluirTurma(Context ctx) {
    if (!Utils.hasPermissionOrThrow(SystemPermission.EXCLUIR_TURMA, ctx))
      return;
    var id = ctx.pathParam("id");
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
