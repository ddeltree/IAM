package poo.iam;

import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Set;

/**
 * O motor de decisão do núcleo.
 *
 * Segue a mesma ordem da AWS: negação explícita vence qualquer concessão, e o
 * padrão, na ausência de ambas, é negar.
 *
 * A política de um principal não é só a dele: percorre-se o grafo de
 * {@link Principal#herdaDe()} somando cláusulas. Isso costumava ser um caso
 * especial escrito aqui dentro — "primeiro o inline, depois cada grupo" —, e
 * virar travessia genérica é o que permite grupos, papéis e sessões usarem o
 * mesmo algoritmo em vez de cada um ganhar um ramo no avaliador.
 */
public final class AccessResolver {

  private static final String INLINE = "inline";

  private AccessResolver() {
  }

  public static boolean isAllowed(Principal principal, Permission permission, Resource resource,
      Object... context) {
    return avaliar(principal, permission, resource, context).permitido();
  }

  /**
   * Como {@link #isAllowed}, mas devolve também qual cláusula decidiu e onde
   * ela estava. É o que sustenta a explicação de um 403.
   */
  public static Decisao avaliar(Principal principal, Permission permission, Resource resource,
      Object... context) {
    if (principal == null)
      return Decisao.negacaoPadrao("nenhum usuário autenticado");

    // Sem esta conferência uma concessão sem condição alcançaria qualquer
    // objeto: o administrador recebe EDITAR_POST irrestrito, e nada impediria
    // essa cláusula de valer sobre uma turma. Recurso nulo é permitido de
    // propósito — é como se pedem ações que não têm alvo, como criar uma turma.
    if (resource != null && !resource.getType().equals(permission.getResourceType()))
      return Decisao.negacaoPadrao(
          permission + " não se aplica a um recurso do tipo " + resource.getType().name());

    // O contexto é montado uma vez e reaproveitado por todas as cláusulas.
    var ctx = ContextResolver.padrao().resolver(principal, resource, context);

    var negacao = procurar(principal, permission, ctx, Effect.DENY);
    if (negacao != null)
      return Decisao.negacaoExplicita(negacao.statement, negacao.origem);

    var concessao = procurar(principal, permission, ctx, Effect.ALLOW);
    if (concessao != null)
      return Decisao.permitido(concessao.statement, concessao.origem);

    return Decisao.negacaoPadrao(
        "nenhuma cláusula concede " + permission + " a " + principal.getName());
  }

  /**
   * A primeira cláusula do efeito pedido que se aplique, procurando no
   * principal e depois em quem ele herda, sempre na mesma ordem.
   */
  private static Achado procurar(Principal raiz, Permission permission, RequestContext ctx,
      Effect efeito) {
    // por identidade, e não por id: um usuário e um grupo podem ter ids iguais
    // sem serem o mesmo principal
    Set<Principal> visitados = Collections.newSetFromMap(new IdentityHashMap<>());
    return procurar(raiz, raiz, permission, ctx, efeito, visitados);
  }

  private static Achado procurar(Principal atual, Principal raiz, Permission permission,
      RequestContext ctx, Effect efeito, Set<Principal> visitados) {
    // o grafo pode ter ciclo; sem isto, um grupo que herda de si mesmo trava
    if (!visitados.add(atual))
      return null;

    for (Statement statement : atual.getStatements()) {
      if (statement.getEffect() == efeito && statement.aplica(permission, ctx))
        return new Achado(statement, atual == raiz ? INLINE : atual.getName());
    }

    for (Principal herdado : atual.herdaDe()) {
      var achado = procurar(herdado, raiz, permission, ctx, efeito, visitados);
      if (achado != null)
        return achado;
    }
    return null;
  }

  private static final class Achado {
    final Statement statement;
    final String origem;

    Achado(Statement statement, String origem) {
      this.statement = statement;
      this.origem = origem;
    }
  }
}
