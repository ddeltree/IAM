package poo.console.api;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;

import io.javalin.Javalin;
import io.javalin.http.Context;
import poo.console.Cenario;
import poo.iam.Policy;
import poo.iam.Statement;
import poo.iam.condition.OperatorRegistry;
import poo.iam.document.ConditionDocument;
import poo.iam.document.PolicyDocument;

/**
 * As políticas nomeadas, que é o que o classroom nunca exercitou: lá elas vêm
 * de um documento na inicialização e não mudam mais.
 */
public class PoliticasController {

  public static void register(Javalin app) {
    app.get("/politicas", PoliticasController::listar);
    app.get("/politicas/documento", PoliticasController::documento);
    app.put("/politicas/{nome}", PoliticasController::salvar);
    app.delete("/politicas/{nome}", PoliticasController::apagar);
    app.post("/politicas/validar-condicao", PoliticasController::validarCondicao);
  }

  private static void listar(Context ctx) {
    var c = Cenario.atual();
    // a mesma forma que o /cenario devolve: duas respostas diferentes para a
    // mesma coisa é como um campo some sem ninguém notar
    ctx.json(c.politicas().stream().map(p -> Json.politicaComAnexos(p, c)).toList());
  }

  /** O documento inteiro, como o {@code GetAccountAuthorizationDetails} da AWS. */
  private static void documento(Context ctx) {
    ctx.json(PolicyDocument.escreverTodas(Cenario.atual().politicas()));
  }

  /**
   * Cria ou substitui uma política.
   *
   * Substituir em vez de editar cláusula a cláusula é consequência de
   * {@code Policy} ser imutável no núcleo — e isso é de propósito: anexar a
   * mesma política a dez principais não pode abrir dez caminhos para alterá-la
   * sem querer. Quem tinha a antiga anexada passa a ter a nova.
   */
  private static void salvar(Context ctx) {
    var c = Cenario.atual();
    var nome = ctx.pathParam("nome");
    var corpo = VocabularioController.corpo(ctx);

    var statements = new LinkedHashSet<Statement>();
    var lidos = corpo.get("statements");
    if (lidos instanceof List<?> lista) {
      for (Object item : lista)
        statements.add(PolicyDocument.lerStatement(item, OperatorRegistry.padrao()));
    }

    var nova = new Policy(nome, statements);
    var antiga = c.politica(nome);
    c.salvarPolitica(nova);

    // reanexar onde a antiga estava: a tela editou "a política X", e ver o
    // acesso sumir de todo mundo por causa disso seria surpresa pura
    if (antiga != null) {
      c.usuarios().forEach(u -> {
        if (u.desanexar(antiga))
          u.anexar(nova);
      });
      c.grupos().forEach(g -> {
        if (g.desanexar(antiga))
          g.anexar(nova);
      });
    }
    ctx.json(Json.politica(nova));
  }

  private static void apagar(Context ctx) {
    ctx.status(Cenario.atual().apagarPolitica(ctx.pathParam("nome")) ? 204 : 404);
  }

  /**
   * Confere se uma condição é legível, e diz quais chaves ela lê.
   *
   * O editor da tela monta a condição em árvore, então o formato sai sempre
   * bem-formado — mas o operador pode não existir, e a lista de chaves lidas é
   * o que permite avisar "esta condição fala de uma chave que nenhum recurso
   * tem", que é o erro que mais parece bug.
   */
  private static void validarCondicao(Context ctx) {
    try {
      var condicao = ConditionDocument.ler(VocabularioController.corpo(ctx).get("condition"));
      var conhecidas = Cenario.atual().chavesDisponiveis();
      var desconhecidas = condicao.chaves().stream()
          .filter(chave -> !conhecidas.contains(chave))
          .toList();
      ctx.json(Map.of(
          "valida", true,
          "chaves", List.copyOf(condicao.chaves()),
          "chavesDesconhecidas", desconhecidas));
    } catch (RuntimeException e) {
      ctx.json(Map.of("valida", false, "erro", String.valueOf(e.getMessage())));
    }
  }
}
