package poo.console.api;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import io.javalin.Javalin;
import io.javalin.http.Context;
import poo.console.Cenario;
import poo.console.RecursoLivre;

/**
 * O cenário inteiro numa resposta só.
 *
 * A tela precisa de quase tudo ao mesmo tempo — os seletores de principal, de
 * permissão e de recurso aparecem em cinco lugares —, e pedir isso em oito
 * requisições faria cada clique piscar. Este é o {@code GET} que o app carrega
 * uma vez e revalida depois de cada mudança.
 */
public class CenarioController {

  public static void register(Javalin app) {
    app.get("/cenario", CenarioController::tudo);
    app.post("/cenario/reiniciar", CenarioController::reiniciar);
  }

  private static void tudo(Context ctx) {
    var c = Cenario.atual();
    var res = new LinkedHashMap<String, Object>();

    res.put("usuarios", c.usuarios().stream().map(u -> Json.principal(u, c)).toList());
    res.put("grupos", c.grupos().stream().map(g -> Json.principal(g, c)).toList());
    res.put("papeis", c.papeis().stream().map(p -> Json.principal(p, c)).toList());
    res.put("sessoes", c.sessoes().stream().map(s -> Json.principal(s, c)).toList());

    res.put("politicas", c.politicas().stream().map(p -> Json.politicaComAnexos(p, c)).toList());
    res.put("recursos", c.recursos().stream().map(CenarioController::recurso).toList());

    res.put("acoes", List.copyOf(c.acoes()));
    res.put("tipos", List.copyOf(c.tipos()));
    res.put("permissoes", c.permissoes().stream().map(Json::permissao).toList());
    res.put("semAlvo", c.semAlvo().stream().map(Json::permissao).toList());

    ctx.json(res);
  }

  static Map<String, Object> recurso(RecursoLivre r) {
    var m = new LinkedHashMap<String, Object>();
    m.put("ref", r.getRef());
    m.put("tipo", r.getTipo());
    m.put("id", r.getId());
    m.put("atributos", r.getAtributos());
    m.put("pai", r.getPaiRef());
    m.put("politica", r.getPolitica() == null ? null : Json.politica(r.getPolitica()));
    return m;
  }

  private static void reiniciar(Context ctx) {
    Cenario.reiniciar();
    ctx.status(204);
  }
}
