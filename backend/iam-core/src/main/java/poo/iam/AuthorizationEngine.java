package poo.iam;

import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Set;

import poo.iam.spi.ResourcePolicyProvider;

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
 *
 * <h2>Por que é instância</h2>
 *
 * Isto já foi uma classe de métodos estáticos que lia um {@link
 * ContextResolver} global. Funcionava enquanto houvesse um sistema só por
 * processo — mas um componente reutilizável não pode assumir isso: duas
 * aplicações no mesmo JVM registrariam provedores de atributos uma por cima da
 * outra, e a segunda a subir venceria em silêncio. Cada motor carrega o
 * contexto dele. Monte um com {@link IamFactory}.
 */
public final class AuthorizationEngine {

  private static final String INLINE = "inline";

  private final ContextResolver contexto;
  private final ResourcePolicyProvider politicasDeRecurso;

  public AuthorizationEngine(ContextResolver contexto) {
    this(contexto, null);
  }

  public AuthorizationEngine(ContextResolver contexto, ResourcePolicyProvider politicasDeRecurso) {
    this.contexto = contexto == null ? new ContextResolver() : contexto;
    this.politicasDeRecurso = politicasDeRecurso;
  }

  /** Como o núcleo lê os atributos dos recursos desta aplicação. */
  public ContextResolver getContexto() {
    return contexto;
  }

  public boolean isAllowed(Principal principal, Permission permission, Resource resource) {
    return avaliar(principal, permission, resource).permitido();
  }

  public boolean isAllowed(Principal principal, Permission permission, Resource resource,
      java.util.Map<String, java.util.List<String>> chavesDaRequisicao) {
    return avaliar(principal, permission, resource, chavesDaRequisicao).permitido();
  }

  public Decisao avaliar(Principal principal, Permission permission, Resource resource) {
    return avaliar(principal, permission, resource, java.util.Map.of());
  }

  /**
   * Como {@link #isAllowed}, mas devolve também qual cláusula decidiu e onde
   * ela estava. É o que sustenta a explicação de um 403.
   */
  /**
   * @param chavesDaRequisicao o que só o chamador sabe — origem, horário do
   *        pedido, cabeçalhos. Chegam ao contexto prefixadas com
   *        {@code requisicao:}.
   */
  public Decisao avaliar(Principal principal, Permission permission, Resource resource,
      java.util.Map<String, java.util.List<String>> chavesDaRequisicao) {
    if (principal == null)
      return Decisao.negacaoPadrao("nenhum usuário autenticado");

    // Sem esta conferência uma concessão sem condição alcançaria qualquer
    // objeto: o administrador recebe EDITAR_POST irrestrito, e nada impediria
    // essa cláusula de valer sobre uma turma. Recurso nulo é permitido de
    // propósito — é como se pedem ações que não têm alvo, como criar uma turma.
    if (resource != null && !resource.getType().name().equals(permission.getResourceType().name()))
      return Decisao.negacaoPadrao(
          permission + " não se aplica a um recurso do tipo " + resource.getType().name());

    // O contexto é montado uma vez e reaproveitado por todas as cláusulas.
    var ctx = contexto.resolver(principal, resource, chavesDaRequisicao);

    // negação explícita vence, venha da identidade ou do recurso
    var negacao = procurar(principal, resource, permission, ctx, Effect.DENY);
    if (negacao != null)
      return Decisao.negacaoExplicita(negacao.statement, negacao.origem);

    var concessao = procurar(principal, resource, permission, ctx, Effect.ALLOW);
    if (concessao != null)
      return Decisao.permitido(concessao.statement, concessao.origem);

    return Decisao.negacaoPadrao(
        "nenhuma cláusula concede " + permission + " a " + principal.getName());
  }

  /**
   * A primeira cláusula do efeito pedido que se aplique, procurando no
   * principal e depois em quem ele herda, sempre na mesma ordem.
   */
  private Achado procurar(Principal raiz, Resource resource, Permission permission,
      RequestContext ctx, Effect efeito) {
    // por identidade, e não por id: um usuário e um grupo podem ter ids iguais
    // sem serem o mesmo principal
    Set<Principal> visitados = Collections.newSetFromMap(new IdentityHashMap<>());
    var naIdentidade = procurar(raiz, raiz, permission, ctx, efeito, visitados);
    if (naIdentidade != null)
      return naIdentidade;
    return noRecurso(resource, permission, ctx, efeito);
  }

  /**
   * A política anexada ao próprio recurso, avaliada junto com a de identidade.
   *
   * Quem ela alcança está dito nas condições dela, sobre {@code principal:*} —
   * não há campo Principal no statement, e não precisa haver.
   */
  private Achado noRecurso(Resource resource, Permission permission, RequestContext ctx,
      Effect efeito) {
    if (politicasDeRecurso == null || resource == null)
      return null;
    var policy = politicasDeRecurso.politicaDe(resource);
    if (policy == null)
      return null;

    for (Statement statement : policy.getStatements()) {
      if (statement.getEffect() == efeito && statement.aplica(permission, ctx))
        return new Achado(statement, resource.getType().name() + "/" + resource.getId());
    }
    return null;
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
