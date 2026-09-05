package poo.console.api;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import io.javalin.Javalin;
import io.javalin.http.Context;
import poo.console.Cenario;
import poo.iam.condition.Operadores;

/**
 * O vocabulário: as ações, os tipos, as permissões e — o que faz o editor de
 * condição funcionar — as chaves e os operadores disponíveis.
 *
 * Sem estas duas listas, escrever uma condição na tela seria adivinhar o nome
 * de uma chave e o de um operador. Chave errada avalia falso em silêncio, o que
 * parece bug e é erro de digitação; operador errado estoura na hora, o que é
 * melhor mas ainda assim evitável.
 */
public class VocabularioController {

  public static void register(Javalin app) {
    app.get("/vocabulario/chaves", VocabularioController::chaves);
    app.get("/vocabulario/operadores", VocabularioController::operadores);
    app.post("/vocabulario/acoes", VocabularioController::criarAcao);
    app.post("/vocabulario/tipos", VocabularioController::criarTipo);
    app.post("/vocabulario/permissoes", VocabularioController::criarPermissao);
    app.delete("/vocabulario/permissoes/{acao}/{tipo}", VocabularioController::apagarPermissao);
  }

  private static void chaves(Context ctx) {
    ctx.json(Cenario.atual().chavesDisponiveis());
  }

  /**
   * Os operadores embutidos, e os prefixos separados deles.
   *
   * Prefixo não é operador: é decorador sobre um, e eles se compõem. A tela
   * precisa da distinção para oferecer "Igual" numa lista e "para algum valor"
   * como uma caixa de seleção ao lado, em vez de listar as doze combinações.
   */
  private static void operadores(Context ctx) {
    var nomes = new ArrayList<String>();
    for (var operador : List.of(Operadores.IGUAL, Operadores.DIFERENTE, Operadores.PARECIDO,
        Operadores.BOOLEANO, Operadores.NULO, Operadores.MAIOR, Operadores.MAIOR_OU_IGUAL,
        Operadores.MENOR, Operadores.MENOR_OU_IGUAL, Operadores.DATA_DEPOIS,
        Operadores.DATA_ANTES))
      nomes.add(operador.name());

    ctx.json(Map.of(
        "operadores", nomes,
        "prefixos", List.of(Operadores.PARA_ALGUM_VALOR, Operadores.PARA_TODO_VALOR,
            Operadores.SE_EXISTIR)));
  }

  private static void criarAcao(Context ctx) {
    Cenario.atual().declararAcao(texto(ctx, "nome"));
    ctx.status(201);
  }

  private static void criarTipo(Context ctx) {
    Cenario.atual().declararTipo(texto(ctx, "nome"));
    ctx.status(201);
  }

  private static void criarPermissao(Context ctx) {
    var corpo = corpo(ctx);
    var semAlvo = Boolean.TRUE.equals(corpo.get("semAlvo"));
    var permissao = Cenario.atual()
        .declararPermissao(texto(corpo, "acao"), texto(corpo, "tipo"), semAlvo);
    ctx.status(201).json(Json.permissao(permissao));
  }

  private static void apagarPermissao(Context ctx) {
    var removeu = Cenario.atual()
        .removerPermissao(ctx.pathParam("acao"), ctx.pathParam("tipo"));
    ctx.status(removeu ? 204 : 404);
  }

  // ---------- leitura de corpo, com erro que diz o que faltou ----------

  @SuppressWarnings("unchecked")
  static Map<String, Object> corpo(Context ctx) {
    var corpo = ctx.bodyAsClass(Map.class);
    return corpo == null ? Map.of() : (Map<String, Object>) corpo;
  }

  static String texto(Context ctx, String campo) {
    return texto(corpo(ctx), campo);
  }

  static String texto(Map<String, Object> corpo, String campo) {
    var valor = corpo.get(campo);
    if (valor == null || String.valueOf(valor).isBlank())
      throw new IllegalArgumentException("Faltou o campo " + campo);
    return String.valueOf(valor);
  }
}
