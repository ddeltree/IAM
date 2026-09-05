package poo.console.api;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import io.javalin.Javalin;
import io.javalin.http.Context;
import poo.console.Cenario;
import poo.console.RecursoLivre;
import poo.iam.Explicacao;
import poo.iam.Permission;
import poo.iam.Principal;
import poo.iam.Resource;
import poo.iam.User;
import poo.iam.query.PredicateRenderer;
import poo.iam.query.SqlWhereRenderer;
import poo.iam.spi.SqlMapping;

/**
 * As perguntas. É por elas que este sistema se distingue de um CRUD de
 * permissões.
 *
 * <ul>
 *   <li><b>simular</b> — pode? e, mais importante, por que não;</li>
 *   <li><b>efetivas</b> — tudo o que se pode sobre um recurso, de uma vez;</li>
 *   <li><b>quem-pode</b> — a pergunta ao contrário;</li>
 *   <li><b>onde-posso</b> — o dual, que devolve um filtro e não uma lista.</li>
 * </ul>
 */
public class ConsultasController {

  public static void register(Javalin app) {
    app.post("/simular", ConsultasController::simular);
    app.get("/efetivas", ConsultasController::efetivas);
    app.get("/quem-pode", ConsultasController::quemPode);
    app.get("/onde-posso", ConsultasController::ondePosso);
  }

  /**
   * O simulador — o {@code SimulatePrincipalPolicy} da AWS.
   *
   * A resposta tem três blocos, e o terceiro é o que faz o desenho ficar
   * visível: sem as chaves resolvidas, "a condição não passou" não diz qual
   * valor foi comparado com qual, e quem está montando a política fica
   * adivinhando.
   */
  private static void simular(Context ctx) {
    var c = Cenario.atual();
    var corpo = VocabularioController.corpo(ctx);

    var principal = PrincipaisController.exigirPrincipal(c,
        VocabularioController.texto(corpo, "principal"));
    var permissao = exigirPermissao(c, VocabularioController.texto(corpo, "acao"),
        VocabularioController.texto(corpo, "tipo"));
    var recurso = corpo.get("recurso") == null ? null
        : exigirRecurso(c, String.valueOf(corpo.get("recurso")));

    var explicacao = c.iam().motor()
        .explicar(principal, permissao, recurso, chavesDaRequisicao(corpo.get("chaves")));

    ctx.json(comoJson(explicacao));
  }

  static Map<String, Object> comoJson(Explicacao explicacao) {
    var decisao = explicacao.getDecisao();
    var res = new LinkedHashMap<String, Object>();

    res.put("permitido", decisao.permitido());
    res.put("tipo", decisao.getTipo().name());
    res.put("motivo", decisao.getMotivo());
    res.put("origem", decisao.getOrigem());
    res.put("decisiva", decisao.getDecisiva() == null ? null
        : Json.statement(decisao.getDecisiva()));

    var clausulas = new ArrayList<Object>();
    for (Explicacao.ClausulaAvaliada c : explicacao.getClausulas()) {
      var m = new LinkedHashMap<String, Object>(Json.statement(c.getStatement()));
      m.put("origem", c.getOrigem());
      // separados de propósito: "não vale" por mirar outro recurso e "não vale"
      // por a condição falhar são problemas diferentes com o mesmo sintoma
      m.put("alcancaORecurso", c.alcancaORecurso());
      m.put("condicaoPassou", c.condicaoPassou());
      m.put("aplicaria", c.aplicaria());
      m.put("decisiva", c.isDecisiva());
      clausulas.add(m);
    }
    res.put("clausulas", clausulas);
    res.put("clausulasAlcancadas", explicacao.getClausulasAlcancadas());

    res.put("contexto", explicacao.getContexto() == null ? Map.of()
        : explicacao.getContexto().getValores());
    return res;
  }

  /** Tudo o que este principal pode sobre este recurso, de uma vez. */
  private static void efetivas(Context ctx) {
    var c = Cenario.atual();
    var principal = PrincipaisController.exigirPrincipal(c, exigirParam(ctx, "principal"));
    var ref = ctx.queryParam("recurso");

    if (ref == null || ref.isBlank()) {
      ctx.json(c.iam().efetivas().globais(principal));
      return;
    }
    ctx.json(c.iam().efetivas().sobre(principal, exigirRecurso(c, ref)));
  }

  /**
   * Quem pode isto?
   *
   * Os que chegariam a poder assumindo um papel vêm num bloco à parte, e não
   * somados: "quem pode" e "quem consegue chegar a poder" são perguntas
   * diferentes, e uma auditoria que as confunde superestima o acesso atual.
   */
  private static void quemPode(Context ctx) {
    var c = Cenario.atual();
    var permissao = exigirPermissao(c, exigirParam(ctx, "acao"), exigirParam(ctx, "tipo"));
    var ref = ctx.queryParam("recurso");
    var recurso = ref == null || ref.isBlank() ? null : exigirRecurso(c, ref);

    var resultado = c.iam().consultas().quemPode(permissao, recurso);

    var viaPapel = new LinkedHashMap<String, Object>();
    resultado.viaPapel.forEach((papel, usuarios) ->
        viaPapel.put(papel, usuarios.stream().map(ConsultasController::resumo).toList()));

    ctx.json(Map.of(
        "principais", resultado.principais.stream().map(ConsultasController::resumo).toList(),
        "viaPapel", viaPapel,
        "avaliados", resultado.avaliados,
        "conhecidos", resultado.conhecidos,
        "podou", resultado.podou));
  }

  /**
   * Sobre o que posso agir?
   *
   * Devolve o filtro em três formas — a restrição legível, os recursos que
   * sobrevivem a ela, e o {@code WHERE} correspondente. As três são a mesma
   * coisa, e é esse o ponto.
   */
  private static void ondePosso(Context ctx) {
    var c = Cenario.atual();
    var principal = PrincipaisController.exigirPrincipal(c, exigirParam(ctx, "principal"));
    var permissao = exigirPermissao(c, exigirParam(ctx, "acao"), exigirParam(ctx, "tipo"));

    if (!(principal instanceof User user))
      throw new IllegalArgumentException(
          "A consulta ao contrário parte de um usuário; " + principal.getName() + " não é um");

    var filtro = c.iam().consultas().ondePosso(user, permissao);
    var predicado = PredicateRenderer.render(filtro, c.iam().contexto());

    var doTipo = c.recursos().stream()
        .filter(r -> r.getTipo().equals(permissao.getResourceType().name()))
        .toList();

    var alcancados = c.iam().consultas().filtrar(user, permissao, doTipo);

    ctx.json(Map.of(
        "restricao", filtro.toString(),
        "sql", SqlWhereRenderer.render(filtro, SQL),
        "recursos", alcancados.stream().map(RecursoLivre::getRef).toList(),
        // quantos o filtro deixa passar antes de o motor confirmar: é o que
        // mostra que a poda escolhe candidatos e não decide
        "candidatos", doTipo.stream().filter(predicado).map(RecursoLivre::getRef).toList()));
  }

  /**
   * A tradução de chave para coluna, num domínio de vocabulário livre.
   *
   * Não há esquema para mapear, então a convenção é direta: {@code recurso:x}
   * vira {@code recurso.x}, e {@code bucket:x} vira {@code bucket.x}. Num
   * sistema de verdade este é o lugar onde mora o mapeamento objeto-relacional
   * das políticas — aqui basta para o SQL sair legível na tela.
   */
  private static final SqlMapping SQL = new SqlMapping() {
    public String igual(String chave, String valor) {
      return coluna(chave) + " = " + literal(valor);
    }

    public String contem(String chave, String valor) {
      return literal(valor) + " = ANY(" + coluna(chave) + ")";
    }

    @Override
    public String compara(String chave, String operador, String valor) {
      return coluna(chave) + " " + operador + " " + literal(valor);
    }

    private String coluna(String chave) {
      return chave.replace(':', '.');
    }

    private String literal(String valor) {
      return "'" + valor.replace("'", "''") + "'";
    }
  };

  // ---------- utilidades ----------

  private static Map<String, Object> resumo(User user) {
    return Map.of("id", user.getId(), "nome", String.valueOf(user.getName()));
  }

  private static String exigirParam(Context ctx, String nome) {
    var valor = ctx.queryParam(nome);
    if (valor == null || valor.isBlank())
      throw new IllegalArgumentException("Faltou o parâmetro " + nome);
    return valor;
  }

  static Permission exigirPermissao(Cenario c, String acao, String tipo) {
    var permissao = c.permissao(acao, tipo);
    if (permissao == null)
      throw new IllegalArgumentException(
          "A permissão " + acao + " sobre " + tipo + " não está declarada no vocabulário");
    return permissao;
  }

  static Resource exigirRecurso(Cenario c, String ref) {
    var recurso = c.recurso(ref);
    if (recurso == null)
      throw new IllegalArgumentException("Não existe recurso " + ref);
    return recurso;
  }

  /** As chaves que só quem chama sabe — aqui, digitadas na tela. */
  private static Map<String, List<String>> chavesDaRequisicao(Object bruto) {
    var res = new LinkedHashMap<String, List<String>>();
    if (!(bruto instanceof Map<?, ?> mapa))
      return res;
    for (var entrada : mapa.entrySet()) {
      var valor = entrada.getValue();
      if (valor instanceof List<?> lista) {
        var valores = new ArrayList<String>();
        lista.forEach(v -> valores.add(String.valueOf(v)));
        res.put(String.valueOf(entrada.getKey()), valores);
      } else if (valor != null) {
        res.put(String.valueOf(entrada.getKey()), List.of(String.valueOf(valor)));
      }
    }
    return res;
  }
}
