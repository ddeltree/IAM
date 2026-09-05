package poo.console.api;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;

import io.javalin.Javalin;
import io.javalin.http.Context;
import poo.console.Cenario;
import poo.console.RecursoLivre;
import poo.iam.Policy;
import poo.iam.Statement;
import poo.iam.condition.OperatorRegistry;
import poo.iam.document.PolicyDocument;

/**
 * Os recursos, com os atributos que você digitou.
 *
 * Aqui está a diferença de fundo para o classroom: lá "os atributos de uma
 * turma" é uma decisão escrita em código, e uma condição só pode falar do que
 * alguém previu. Aqui o atributo nasce na tela, e a condição pode falar dele no
 * minuto seguinte.
 */
public class RecursosController {

  public static void register(Javalin app) {
    app.get("/recursos", RecursosController::listar);
    app.put("/recursos/{tipo}/{id}", RecursosController::salvar);
    app.delete("/recursos/{tipo}/{id}", RecursosController::apagar);
  }

  private static void listar(Context ctx) {
    ctx.json(Cenario.atual().recursos().stream().map(CenarioController::recurso).toList());
  }

  private static void salvar(Context ctx) {
    var c = Cenario.atual();
    var corpo = VocabularioController.corpo(ctx);
    var recurso = new RecursoLivre(ctx.pathParam("tipo"), ctx.pathParam("id"));

    recurso.setAtributos(atributos(corpo.get("atributos")));

    var pai = corpo.get("pai");
    if (pai != null && !String.valueOf(pai).isBlank()) {
      var ref = String.valueOf(pai);
      if (ref.equals(recurso.getRef()))
        throw new IllegalArgumentException("Um recurso não pode ser pai de si mesmo");
      recurso.setPaiRef(ref);
    }

    var politica = corpo.get("politica");
    if (politica instanceof Map<?, ?> mapa && mapa.get("statements") instanceof List<?> lista) {
      var statements = new LinkedHashSet<Statement>();
      for (Object item : lista)
        statements.add(PolicyDocument.lerStatement(item, OperatorRegistry.padrao()));
      // o nome da política de recurso é a própria referência: ela não é
      // anexável a mais ninguém, então um nome próprio só confundiria
      recurso.setPolitica(statements.isEmpty() ? null
          : new Policy(recurso.getRef(), statements));
    }

    c.salvarRecurso(recurso);
    ctx.json(CenarioController.recurso(recurso));
  }

  /**
   * Os atributos chegam como {@code {"dono": "ana"}} ou
   * {@code {"tags": ["a","b"]}} — a tela permite as duas formas porque
   * multivalorado é o caso raro, e obrigar uma lista de um elemento para tudo
   * seria ruído em cada linha.
   */
  private static Map<String, List<String>> atributos(Object bruto) {
    var res = new LinkedHashMap<String, List<String>>();
    if (!(bruto instanceof Map<?, ?> mapa))
      return res;

    for (var entrada : mapa.entrySet()) {
      var chave = String.valueOf(entrada.getKey());
      var valor = entrada.getValue();
      if (valor instanceof List<?> lista) {
        var valores = new ArrayList<String>();
        lista.forEach(v -> valores.add(String.valueOf(v)));
        res.put(chave, valores);
      } else if (valor != null) {
        res.put(chave, List.of(String.valueOf(valor)));
      }
    }
    return res;
  }

  private static void apagar(Context ctx) {
    var ref = ctx.pathParam("tipo") + "/" + ctx.pathParam("id");
    ctx.status(Cenario.atual().apagarRecurso(ref) ? 204 : 404);
  }
}
