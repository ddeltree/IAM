package poo.console.api;

import java.util.Map;

import io.javalin.Javalin;
import io.javalin.http.Context;
import poo.console.Cenario;
import poo.iam.Group;
import poo.iam.MembershipManager;
import poo.iam.Role;
import poo.iam.Statement;
import poo.iam.User;
import poo.iam.condition.OperatorRegistry;
import poo.iam.document.PolicyDocument;

/** Usuários, grupos, papéis: criar, apagar, e mexer na política de cada um. */
public class PrincipaisController {

  public static void register(Javalin app) {
    app.post("/usuarios", ctx -> criar(ctx, "USUARIO"));
    app.post("/grupos", ctx -> criar(ctx, "GRUPO"));
    app.post("/papeis", ctx -> criar(ctx, "PAPEL"));

    app.get("/principais/{id}", PrincipaisController::ver);
    app.delete("/principais/{id}", PrincipaisController::apagar);

    app.post("/usuarios/{id}/grupos/{grupo}", ctx -> membro(ctx, true));
    app.delete("/usuarios/{id}/grupos/{grupo}", ctx -> membro(ctx, false));

    app.post("/principais/{id}/politicas/{nome}", ctx -> anexar(ctx, true));
    app.delete("/principais/{id}/politicas/{nome}", ctx -> anexar(ctx, false));

    app.post("/principais/{id}/statements", PrincipaisController::adicionarStatement);
    app.delete("/principais/{id}/statements/{sid}", PrincipaisController::removerStatement);

    app.post("/papeis/{id}/confianca", PrincipaisController::adicionarConfianca);
    app.delete("/papeis/{id}/confianca/{sid}", PrincipaisController::removerConfianca);
    app.post("/papeis/{id}/assumir", PrincipaisController::assumir);
    app.delete("/sessoes/{id}", PrincipaisController::largar);
  }

  private static void criar(Context ctx, String tipo) {
    var c = Cenario.atual();
    var corpo = VocabularioController.corpo(ctx);
    var nome = VocabularioController.texto(corpo, "nome");
    // o id vem do nome quando não é informado: URLs legíveis valem mais que
    // um contador, e o núcleo já aceita id de fora desde que deixou de impor
    // o dele
    var id = corpo.containsKey("id") ? VocabularioController.texto(corpo, "id") : slug(nome);

    var criado = switch (tipo) {
      case "USUARIO" -> c.criarUsuario(id, nome);
      case "GRUPO" -> c.criarGrupo(id, nome);
      default -> c.criarPapel(id, nome);
    };
    ctx.status(201).json(Json.principal(criado, c));
  }

  static String slug(String nome) {
    return java.text.Normalizer.normalize(nome, java.text.Normalizer.Form.NFD)
        .replaceAll("\\p{M}", "")
        .toLowerCase()
        .replaceAll("[^a-z0-9]+", "-")
        .replaceAll("^-|-$", "");
  }

  private static void ver(Context ctx) {
    var c = Cenario.atual();
    ctx.json(Json.principal(exigirPrincipal(c, ctx.pathParam("id")), c));
  }

  private static void apagar(Context ctx) {
    ctx.status(Cenario.atual().apagarPrincipal(ctx.pathParam("id")) ? 204 : 404);
  }

  private static void membro(Context ctx, boolean entrar) {
    var c = Cenario.atual();
    var user = exigirUsuario(c, ctx.pathParam("id"));
    var grupo = exigirGrupo(c, ctx.pathParam("grupo"));
    if (entrar)
      MembershipManager.link(user, grupo);
    else
      MembershipManager.unlink(user, grupo);
    ctx.status(204);
  }

  private static void anexar(Context ctx, boolean anexar) {
    var c = Cenario.atual();
    var principal = exigirPrincipal(c, ctx.pathParam("id"));
    var policy = c.politica(ctx.pathParam("nome"));
    if (policy == null)
      throw new IllegalArgumentException("Política desconhecida: " + ctx.pathParam("nome"));

    boolean mudou;
    if (principal instanceof User u)
      mudou = anexar ? u.anexar(policy) : u.desanexar(policy);
    else if (principal instanceof Group g)
      mudou = anexar ? g.anexar(policy) : g.desanexar(policy);
    else if (principal instanceof Role p)
      // um papel não desanexa: a política dele é o que ele concede a quem o
      // assume, e mexer nisso é editar o papel
      mudou = anexar && p.anexar(policy);
    else
      throw new IllegalArgumentException("Uma sessão não tem política própria para anexar");
    ctx.status(mudou ? 204 : 409).result(mudou ? "" : "Nada mudou: a política já estava assim");
  }

  private static void adicionarStatement(Context ctx) {
    var c = Cenario.atual();
    var principal = exigirPrincipal(c, ctx.pathParam("id"));
    var statement = lerStatement(ctx);

    boolean mudou;
    if (principal instanceof User u)
      mudou = u.add(statement);
    else if (principal instanceof Group g)
      mudou = g.add(statement);
    else if (principal instanceof Role p)
      mudou = p.add(statement);
    else
      throw new IllegalArgumentException("Uma sessão não tem política própria");

    // Statement.equals ignora o sid de propósito — sid é rótulo, não conteúdo.
    // Numa tela isso apareceria como "cliquei em adicionar e não aconteceu
    // nada", então é preciso dizer
    if (!mudou)
      throw new IllegalStateException(
          "Essa cláusula já existe neste principal, com o sid " + statement.getSid());
    ctx.status(201).json(Json.statement(statement));
  }

  private static void removerStatement(Context ctx) {
    var principal = exigirPrincipal(Cenario.atual(), ctx.pathParam("id"));
    var sid = ctx.pathParam("sid");
    boolean removeu;
    if (principal instanceof User u)
      removeu = u.removerPorSid(sid);
    else if (principal instanceof Group g)
      removeu = g.removerPorSid(sid);
    else if (principal instanceof Role p)
      removeu = p.removerPorSid(sid);
    else
      removeu = false;
    ctx.status(removeu ? 204 : 404);
  }

  private static void adicionarConfianca(Context ctx) {
    var papel = exigirPapel(Cenario.atual(), ctx.pathParam("id"));
    var statement = lerStatement(ctx);
    if (!papel.confiaEm(statement))
      throw new IllegalStateException("Essa autorização de assumir já existe");
    ctx.status(201).json(Json.statement(statement));
  }

  private static void removerConfianca(Context ctx) {
    var papel = exigirPapel(Cenario.atual(), ctx.pathParam("id"));
    ctx.status(papel.deixaDeConfiar(ctx.pathParam("sid")) ? 204 : 404);
  }

  /**
   * Assumir um papel. A decisão é do motor, pela política de confiança — que é
   * uma política de recurso, com o papel como recurso.
   */
  private static void assumir(Context ctx) {
    var c = Cenario.atual();
    var papel = exigirPapel(c, ctx.pathParam("id"));
    var quem = exigirPrincipal(c, VocabularioController.texto(ctx, "principal"));

    var decisao = c.iam().papeis().podeAssumir(quem, papel);
    if (!decisao.permitido()) {
      ctx.status(403).json(Map.of(
          "permitido", false,
          "motivo", decisao.getMotivo(),
          "tipo", decisao.getTipo().name()));
      return;
    }

    var sessao = c.iam().papeis().assumir(quem, papel);
    c.guardarSessao(sessao);
    ctx.status(201).json(Json.principal(sessao, c));
  }

  private static void largar(Context ctx) {
    ctx.status(Cenario.atual().largarSessao(ctx.pathParam("id")) ? 204 : 404);
  }

  // ---------- buscas que falham dizendo o que não achou ----------

  static poo.iam.Principal exigirPrincipal(Cenario c, String id) {
    var principal = c.principal(id);
    if (principal == null)
      throw new IllegalArgumentException("Não existe principal com o id " + id);
    return principal;
  }

  private static User exigirUsuario(Cenario c, String id) {
    var user = c.usuario(id);
    if (user == null)
      throw new IllegalArgumentException("Não existe usuário com o id " + id);
    return user;
  }

  private static Group exigirGrupo(Cenario c, String id) {
    var grupo = c.grupo(id);
    if (grupo == null)
      throw new IllegalArgumentException("Não existe grupo com o id " + id);
    return grupo;
  }

  private static Role exigirPapel(Cenario c, String id) {
    var papel = c.papel(id);
    if (papel == null)
      throw new IllegalArgumentException("Não existe papel com o id " + id);
    return papel;
  }

  static Statement lerStatement(Context ctx) {
    return PolicyDocument.lerStatement(VocabularioController.corpo(ctx),
        OperatorRegistry.padrao());
  }
}
