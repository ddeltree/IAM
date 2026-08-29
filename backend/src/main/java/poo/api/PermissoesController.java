package poo.api;

import java.util.LinkedHashMap;
import java.util.Map;

import io.javalin.Javalin;
import io.javalin.http.Context;
import poo.classroom.iam.ClassroomResource;
import poo.classroom.iam.PermissoesEfetivas;
import poo.classroom.iam.SecurityContext;
import poo.iam.Decisao;
import poo.iam.PrincipalResource;
import poo.iam.Resource;
import poo.iam.User;

/**
 * Expõe o que o usuário autenticado pode fazer, para que a interface não
 * precise reescrever as regras de autorização do lado dela.
 *
 * Isto conta ao usuário também o que ele <em>não</em> pode — é uma escolha
 * deliberada, e o que permite a tela esconder um botão em vez de oferecer uma
 * ação que vai virar 403.
 */
public class PermissoesController {

  public static void register(Javalin app) {
    app.get("/permissoes", PermissoesController::consultar);
  }

  /**
   * {@code GET /permissoes} devolve as ações sem alvo; cada
   * {@code ?recurso=TIPO/id} acrescenta o mapa daquele recurso. Pedir todos os
   * recursos de uma tela numa chamada só evita o N+1 de uma lista de posts.
   */
  private static void consultar(Context ctx) {
    var user = Utils.findAuthUserOrThrow(ctx);
    var explicar = "true".equals(ctx.queryParam("explicar"));

    var resposta = new LinkedHashMap<String, Object>();
    resposta.put("principal", principal(user));
    resposta.put("global", explicar
        ? explicado(PermissoesEfetivas.globaisExplicadas(user))
        : PermissoesEfetivas.globais(user));

    var recursos = new LinkedHashMap<String, Object>();
    for (String ref : ctx.queryParams("recurso")) {
      var recurso = resolver(ref);
      // Referência desconhecida vira mapa vazio em vez de erro: a tela trata
      // ausência como "não pode", que é o lado seguro.
      if (recurso == null) {
        recursos.put(ref, Map.of());
        continue;
      }
      recursos.put(ref, explicar
          ? explicado(PermissoesEfetivas.explicadas(user, recurso))
          : PermissoesEfetivas.sobre(user, recurso));
    }
    resposta.put("recursos", recursos);

    ctx.json(resposta);
  }

  /** Quem está autenticado, para a interface não inferir o papel de um 403. */
  private static Map<String, Object> principal(User user) {
    var auth = SecurityContext.getInstance();
    var papel = auth.isAdmin(user) ? "ADMIN"
        : auth.isProfessor(user) ? "PROFESSOR"
            : auth.isAluno(user) ? "ALUNO" : "DESCONHECIDO";
    return Map.of("id", user.getId(), "name", user.getName(), "papel", papel);
  }

  private static Map<String, Object> explicado(Map<String, Decisao> decisoes) {
    var res = new LinkedHashMap<String, Object>();
    decisoes.forEach((acao, d) -> {
      var item = new LinkedHashMap<String, Object>();
      item.put("permitido", d.permitido());
      item.put("motivo", d.getMotivo());
      if (d.getDecisiva() != null) {
        item.put("sid", d.getDecisiva().getSid());
        item.put("origem", d.getOrigem());
      }
      res.put(acao, item);
    });
    return res;
  }

  /** Resolve a referência {@code TIPO/id}, ou {@code null} se não existir. */
  static Resource resolver(String referencia) {
    if (referencia == null)
      return null;
    var partes = referencia.split("/", 2);
    if (partes.length != 2)
      return null;
    var id = partes[1];
    switch (partes[0].toUpperCase()) {
      case "TURMA":
        return TurmaController.getTurma(id);
      case "POST":
        return PostController.get(id);
      case "ATIVIDADE":
        return AtividadeController.get(id);
      case "COMENTARIO":
        return ComentarioController.get(id);
      case "USUARIO":
        return usuario(id);
      default:
        return null;
    }
  }

  private static User usuario(String id) {
    var auth = SecurityContext.getInstance();
    return auth.isAdmin(id) ? auth.getAdmin() : UserController.getUser(id);
  }

  /** Só para deixar explícito de onde vêm os nomes aceitos na referência. */
  static String referenciaDe(Resource recurso) {
    var tipo = recurso.getType();
    var nome = tipo == PrincipalResource.USUARIO ? "USUARIO" : ((ClassroomResource) tipo).name();
    return nome + "/" + recurso.getId();
  }
}
