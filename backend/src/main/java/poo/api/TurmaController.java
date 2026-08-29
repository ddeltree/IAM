package poo.api;

import io.javalin.Javalin;
import io.javalin.http.Context;
import poo.api.exceptions.NotFoundException;
import poo.classroom.Turma;
import poo.classroom.iam.ClassroomPermission;
import poo.iam.User;

import static poo.classroom.iam.ClassroomPermission.*;

import java.util.*;

public class TurmaController {

  private static final Map<String, Turma> turmas = new HashMap<>();

  public static Turma getTurma(String id) {
    return turmas.get(id);
  }

  /** Todas as turmas. Usado pelas consultas do IAM. */
  public static java.util.Collection<Turma> todas() {
    return turmas.values();
  }

  public static void register(Javalin app) {
    app.get("/turmas", TurmaController::listarTurmas);
    app.get("/turmas/{turmaId}", TurmaController::verTurma);

    app.post("/turmas", TurmaController::criarTurma);

    app.put("/turmas/{turmaId}", TurmaController::atualizarTurma);

    app.delete("/turmas/{turmaId}", TurmaController::excluirTurma);

  }

  private static void listarTurmas(Context ctx) {
    var user = Utils.findAuthUserOrThrow(ctx);
    List<Turma> result = new ArrayList<>();
    for (Turma turma : turmas.values()) {
      // uma permissão só: o que muda por papel é a condição com que cada grupo
      // a recebe (administrador sem restrição, professor e aluno pela turma)
      if (!LISTAR_TURMAS.isAllowed(user, turma))
        continue;
      result.add(turma);
    }
    ctx.json(result);
  }

  private static void verTurma(Context ctx) {
    var turma = findTurmaOrThrow(ctx);
    if (!Utils.hasPermissionOrThrow(ctx, VER_TURMA, turma))
      return;
    ctx.json(turma);
  }

  private static void criarTurma(Context ctx) {
    if (!Utils.hasPermissionOrThrow(ctx, ClassroomPermission.CRIAR_TURMA))
      return;
    TurmaDTO dto = ctx.bodyAsClass(TurmaDTO.class);
    User professor = Utils.findAuthUserOrThrow(ctx);
    Turma turma = new Turma(dto.nome, professor);
    turmas.put(turma.getId(), turma);
    ctx.status(201).json(turma);
  }

  private static void atualizarTurma(Context ctx) {
    var turma = findTurmaOrThrow(ctx);
    if (!Utils.hasPermissionOrThrow(ctx, ClassroomPermission.EDITAR_TURMA, turma))
      return;
    TurmaDTO dto = ctx.bodyAsClass(TurmaDTO.class);
    turma.setNome(dto.nome);
    ctx.status(200).json(turma);
  }

  private static void excluirTurma(Context ctx) {
    var turma = findTurmaOrThrow(ctx);
    if (!Utils.hasPermissionOrThrow(ctx, ClassroomPermission.EXCLUIR_TURMA, turma))
      return;
    turmas.remove(turma.getId());
    PostController.removerPostsDe(turma);
    AtividadeController.removerAtividadesDe(turma);
    ctx.status(204);
  }

  private static Turma findTurmaOrThrow(Context ctx) {
    var idTurma = ctx.pathParam("turmaId");
    return findTurmaOrThrow(idTurma);
  }

  public static Turma findTurmaOrThrow(String idTurma) {
    var turma = turmas.get(idTurma);
    if (turma == null)
      throw new NotFoundException("Turma não encontrada");
    return turma;
  }

  /** Esvazia o repositório em memória. Usado pelos testes. */
  public static void reset() {
    turmas.clear();
  }

  // DTO para simplificar entrada de dados
  public static class TurmaDTO {
    public String nome;
    public String professorId;
  }

}
