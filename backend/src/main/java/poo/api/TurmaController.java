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
    var auth = SecurityContext.getInstance();
    var uid = ctx.queryParam("id");
    if (Utils.isAdmin(uid)) {
      ctx.json(turmas.values());
      return;
    } else if (auth.isProfessor(uid)) {
      var professor = UserController.getUser(uid);
      var turmasProf = TurmaController.turmas.values().stream()
          .filter(turma -> turma.getProfessorResponsavel().equals(professor)).collect(Collectors.toList());
      ctx.json(turmasProf);
      return;
    } else {
      var user = UserController.getUser(uid);
      List<Turma> turmasAluno = TurmaController.turmas.values().stream().filter(turma -> turma.temAluno(user))
          .collect(Collectors.toList());
      ctx.json(turmasAluno);
    }
  }

  private static void listarParticipantes(Context ctx) {
    if (!Utils.hasPermissionOr403(SystemPermission.LISTAR_PARTICIPANTES, ctx))
      return;
    var turma = turmas.get(ctx.pathParam("id"));
    var participantes = turma.getParticipantes();
    ctx.json(participantes.stream().map((User user) -> new Participante(user, turma)).toList());
  }

  private static void verTurma(Context ctx) {
    String id = ctx.pathParam("id");
    Turma turma = turmas.get(id);
    if (turma == null) {
      ctx.status(404).result("Turma não encontrada");
      return;
    }
    ctx.json(turma);
  }

  private static void criarTurma(Context ctx) {
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

  private static void excluirTurma(Context ctx) {
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

  public static class Participante {
    public String turmaId;
    public String userId;
    public String name;

    public Participante(User user, Turma turma) {
      this.turmaId = turma.getId();
      this.userId = user.getId();
      this.name = user.getName();
    }
  }
}
