package poo.api;

import io.javalin.Javalin;
import io.javalin.http.Context;
import poo.iam.MembershipManager;
import poo.iam.SecurityContext;
import poo.iam.User;

import java.util.*;

public class UserController {

  private static final Map<String, User> usuarios = new HashMap<>();

  public static User getUser(String id) {
    return usuarios.get(id);
  }

  private static User getUser(Context ctx) {
    String id = ctx.pathParam("id");
    User user = usuarios.get(id);
    if (user == null) {
      ctx.status(404).result("Usuário não encontrado");
      return null;
    }
    return user;
  }

  public static void register(Javalin app) {
    app.get("/usuarios", UserController::listar);
    app.get("/usuarios/{id}", UserController::ver);
    app.post("/usuarios", UserController::criar);
    // app.put("/usuarios/{id}", UserController::atualizar);
    // app.delete("/usuarios/{id}", UserController::excluir);
  }

  private static void listar(Context ctx) {
    var admin = SecurityContext.getInstance().getAdmin();
    var uid = ctx.queryParam("id");
    var tid = ctx.queryParam("turmaId");
    var turma = Utils.isAdmin(uid) ? null : TurmaController.getTurma(tid);
    if (!Utils.isAdmin(uid) && !Utils.isParticipante(uid, tid)) {
      ctx.status(403).result("Não autorizado");
      return;
    }
    if (!Utils.isAdmin(uid)) {
      ctx.json(turma.getAlunos().stream().map(UserController::toDTO).toList());
      return;
    }
    List<User> users = new ArrayList<>(usuarios.values());
    users.add(admin);
    ctx.json(users.stream().map(UserController::toDTO).toList());
  }

  private static void ver(Context ctx) {
    var user = getUser(ctx);
    ctx.json(toDTO(user));
  }

  private static void criar(Context ctx) {
    UserDTO dto = ctx.bodyAsClass(UserDTO.class);
    var autorId = dto.autorId;
    var auth = SecurityContext.getInstance();
    if (dto.tipo == 1 && auth.isAdmin(autorId) || dto.tipo == 0 && auth.isProfessor(autorId)) {
      User novoUsuario = new User(dto.name);
      MembershipManager.link(novoUsuario, dto.tipo == 1 ? auth.getProfessores() : auth.getAlunos());
      usuarios.put(novoUsuario.getId(), novoUsuario);
      ctx.status(201).json(toDTO(novoUsuario));
      return;
    }
    String err;
    if (dto.tipo == 1) {
      err = "Apenas o ADMIN pode criar professores";
    } else {
      err = "Apenas o PROFESSOR pode criar alunos";
    }
    ctx.status(403).result("Não autorizado: " + err);
    return;
  }

  // private static void atualizar(Context ctx) {
  // var user = getUser(ctx);
  // UserDTO dto = ctx.bodyAsClass(UserDTO.class);
  // user.setName(dto.name);
  // ctx.status(200).json(toDTO(user));
  // }

  // private static void excluir(Context ctx) {
  // var user = getUser(ctx);
  // Utils.isAdmin(ctx.)
  // usuarios.remove(user.getId());
  // ctx.status(204);
  // }

  // DTO para serialização e entrada de dados
  public static class UserDTO {
    public String id;
    public String name;
    public int tipo = 0; // 0 = aluno, 1 = professor
    public String turmaId;
    public String autorId;
  }

  private static UserDTO toDTO(User user) {
    var auth = SecurityContext.getInstance();
    UserDTO dto = new UserDTO();
    dto.id = user.getId();
    dto.name = user.getName();
    dto.tipo = auth.isProfessor(user) ? 1 : auth.isAluno(user) ? 0 : -1;
    return dto;
  }
}
