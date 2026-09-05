package poo.iam;

import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
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
      Map<String, List<String>> chavesDaRequisicao) {
    return avaliar(principal, permission, resource, chavesDaRequisicao).permitido();
  }

  public Decisao avaliar(Principal principal, Permission permission, Resource resource) {
    return avaliar(principal, permission, resource, Map.of());
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
      Map<String, List<String>> chavesDaRequisicao) {
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
   * A primeira cláusula do efeito pedido que se aplique.
   */
  private Achado procurar(Principal raiz, Resource resource, Permission permission,
      RequestContext ctx, Effect efeito) {
    var achado = new Achado[1];
    percorrer(raiz, resource, (statement, origem) -> {
      if (statement.getEffect() != efeito || !statement.aplica(permission, ctx))
        return false;
      achado[0] = new Achado(statement, origem);
      return true; // achou: pode parar
    });
    return achado[0];
  }

  /**
   * Percorre as cláusulas alcançáveis, na ordem em que a decisão as considera:
   * o principal, depois quem ele herda, e por fim a política do próprio
   * recurso. O visitante devolve {@code true} para interromper.
   *
   * Existe como travessia separada porque há dois usos — decidir e
   * {@link #explicar(Principal, Permission, Resource, Map) explicar} — e uma
   * explicação que percorresse a política por conta própria seria um segundo
   * caminho de decisão: divergiria do motor no dia em que alguém mexesse em um
   * dos dois.
   */
  private void percorrer(Principal raiz, Resource resource,
      java.util.function.BiPredicate<Statement, String> visitante) {
    // por identidade, e não por id: um usuário e um grupo podem ter ids iguais
    // sem serem o mesmo principal
    Set<Principal> visitados = Collections.newSetFromMap(new IdentityHashMap<>());
    if (percorrerPrincipal(raiz, raiz, visitados, visitante))
      return;
    percorrerRecurso(resource, visitante);
  }

  private boolean percorrerPrincipal(Principal atual, Principal raiz, Set<Principal> visitados,
      java.util.function.BiPredicate<Statement, String> visitante) {
    // o grafo pode ter ciclo; sem isto, um grupo que herda de si mesmo trava
    if (!visitados.add(atual))
      return false;

    var origem = atual == raiz ? INLINE : atual.getName();
    for (Statement statement : atual.getStatements()) {
      if (visitante.test(statement, origem))
        return true;
    }

    for (Principal herdado : atual.herdaDe()) {
      if (percorrerPrincipal(herdado, raiz, visitados, visitante))
        return true;
    }
    return false;
  }

  /**
   * A política anexada ao próprio recurso. Quem ela alcança está dito nas
   * condições dela, sobre {@code principal:*} — não há campo Principal no
   * statement, e não precisa haver.
   */
  private boolean percorrerRecurso(Resource resource,
      java.util.function.BiPredicate<Statement, String> visitante) {
    if (politicasDeRecurso == null || resource == null)
      return false;
    var policy = politicasDeRecurso.politicaDe(resource);
    if (policy == null)
      return false;

    var origem = resource.getType().name() + "/" + resource.getId();
    for (Statement statement : policy.getStatements()) {
      if (visitante.test(statement, origem))
        return true;
    }
    return false;
  }

  /**
   * Tudo o que o motor considerou para chegar à decisão.
   *
   * {@link Decisao} nomeia a cláusula que decidiu — e quando nenhuma casou, não
   * nomeia nada: "nenhuma cláusula concede" não diz por que as que existiam não
   * serviram. Isto devolve cada cláusula que fala sobre a permissão, se ela
   * alcança este recurso, se a condição dela passou, e o contexto inteiro sobre
   * o qual as condições decidiram. É o {@code MatchedStatements} que o policy
   * simulator da AWS devolve.
   *
   * <b>A decisão aqui é a do motor</b>, obtida chamando
   * {@link #avaliar(Principal, Permission, Resource, Map)} — este método não a
   * recalcula. Só a coleta é nova, e ela usa a mesma travessia que a busca.
   */
  public Explicacao explicar(Principal principal, Permission permission, Resource resource,
      Map<String, List<String>> chavesDaRequisicao) {
    var decisao = avaliar(principal, permission, resource, chavesDaRequisicao);
    if (principal == null)
      return new Explicacao(decisao, null, List.of(), 0);

    var ctx = contexto.resolver(principal, resource, chavesDaRequisicao);
    var clausulas = new ArrayList<Explicacao.ClausulaAvaliada>();
    var total = new int[1];

    percorrer(principal, resource, (statement, origem) -> {
      total[0]++;
      if (!statement.falaSobre(permission))
        return false;

      // separado da condição de propósito: "escrevi a cláusula e ela não vale"
      // tem duas causas bem diferentes, e confundi-las é o que faz alguém
      // procurar erro na condição quando o problema é o recurso mirado
      var alcanca = statement.getResource().casa(ctx.getRecurso());
      var passou = alcanca && statement.getCondition().avaliar(ctx);
      clausulas.add(new Explicacao.ClausulaAvaliada(statement, origem, alcanca, passou,
          statement == decisao.getDecisiva()));
      return false; // coletar tudo, nunca interromper
    });

    return new Explicacao(decisao, ctx, clausulas, total[0]);
  }

  public Explicacao explicar(Principal principal, Permission permission, Resource resource) {
    return explicar(principal, permission, resource, Map.of());
  }

  /**
   * Todas as cláusulas que valem para este principal, cada uma rotulada com de
   * onde vem — as próprias, as dos grupos, as das políticas anexadas.
   *
   * {@link Principal#getStatements()} devolve só as próprias, o que é o que o
   * motor precisa a cada nível da travessia. Quem quer <em>mostrar</em> a
   * política de alguém precisa da soma, e percorrer o grafo por fora seria
   * repetir a travessia — com o detalhe de que esquecer o conjunto de visitados
   * ali trava quem chamar.
   */
  public List<ClausulaDeOrigem> clausulasDe(Principal principal) {
    var res = new ArrayList<ClausulaDeOrigem>();
    if (principal == null)
      return res;
    percorrer(principal, null, (statement, origem) -> {
      res.add(new ClausulaDeOrigem(statement, origem));
      return false;
    });
    return res;
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
