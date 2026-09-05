package poo.iam.query;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.List;
import java.util.Set;

import poo.iam.AuthorizationEngine;
import poo.iam.Effect;
import poo.iam.Group;
import poo.iam.Permission;
import poo.iam.spi.PrincipalDirectory;
import poo.iam.spi.ResourcePolicyProvider;
import poo.iam.RequestContext;
import poo.iam.Resource;
import poo.iam.Role;
import poo.iam.SessionBroker;
import poo.iam.Statement;
import poo.iam.User;

/**
 * A pergunta ao contrário: "quem pode isto?".
 *
 * Há dois caminhos. A varredura simula para todo principal conhecido — é a
 * implementação de referência, sempre correta. A poda usa
 * {@link PrincipalConstraintExtractor} para descobrir, direto das cláusulas,
 * quais principais sequer poderiam ser alcançados.
 *
 * A regra que torna a poda segura: ela só escolhe candidatos, e o motor avalia
 * cada um deles. Um erro no extrator custa desempenho, nunca acesso indevido.
 */
public final class PolicyQuery {

  private final PrincipalDirectory diretorio;
  private final AuthorizationEngine motor;
  private final ResourcePolicyProvider politicasDeRecurso;

  public PolicyQuery(PrincipalDirectory diretorio, AuthorizationEngine motor) {
    this(diretorio, motor, null);
  }

  public PolicyQuery(PrincipalDirectory diretorio, AuthorizationEngine motor,
      ResourcePolicyProvider politicasDeRecurso) {
    this.diretorio = diretorio;
    this.motor = motor;
    this.politicasDeRecurso = politicasDeRecurso;
  }

  /** O resultado, com o rastro de quanto trabalho foi evitado. */
  public static final class Resultado {
    /** Quem pode agora, sem assumir nada. */
    public final List<User> principais;
    /**
     * Quem <em>passaria</em> a poder assumindo um papel, por papel.
     *
     * Fica separado de propósito. Juntar seria mentir sobre o que a resposta
     * significa: "quem pode excluir este post" e "quem consegue chegar a poder
     * excluir este post" são perguntas diferentes, e uma auditoria que as
     * confunde superestima o acesso atual. Quem quiser as duas somadas soma;
     * quem não notar a diferença recebe a resposta estrita.
     */
    public final Map<String, List<User>> viaPapel;
    public final int avaliados;
    public final int conhecidos;
    public final boolean podou;

    Resultado(List<User> principais, Map<String, List<User>> viaPapel, int avaliados,
        int conhecidos, boolean podou) {
      this.principais = principais;
      this.viaPapel = viaPapel;
      this.avaliados = avaliados;
      this.conhecidos = conhecidos;
      this.podou = podou;
    }
  }

  /** Implementação de referência: pergunta ao motor sobre todo mundo. */
  public List<User> quemPodeVarrendo(Permission permissao, Resource recurso) {
    return diretorio.usuarios().stream()
        .filter(u -> motor.isAllowed(u, permissao, recurso))
        .toList();
  }

  public Resultado quemPode(Permission permissao, Resource recurso) {
    var candidatos = candidatos(permissao, recurso);
    var conhecidos = diretorio.usuarios().size();

    // sem poda possível, a lista de candidatos é todo mundo
    var aAvaliar = candidatos == null ? new ArrayList<>(diretorio.usuarios()) : candidatos;

    var permitidos = aAvaliar.stream()
        .filter(u -> motor.isAllowed(u, permissao, recurso))
        .toList();

    return new Resultado(permitidos, viaPapel(permissao, recurso), aAvaliar.size(), conhecidos,
        candidatos != null);
  }

  /**
   * Quem chegaria a poder assumindo um papel.
   *
   * É varredura, sem poda: para cada papel, quem tem confiança para assumi-lo,
   * e depois se a sessão resultante alcança a permissão. Podar aqui exigiria
   * extrair restrição de duas políticas encadeadas, e o ganho não pagaria — a
   * quantidade de papéis é pequena por natureza.
   *
   * O detalhe que surpreende: sob uma sessão, {@code principal:id} é o id da
   * sessão, não o da pessoa. Uma condição como "o autor pode editar" portanto
   * <b>não</b> vale para quem assumiu um papel — o que está certo, e é
   * exatamente o que separa agir como si mesmo de agir sob um papel.
   */
  private Map<String, List<User>> viaPapel(Permission permissao, Resource recurso) {
    var papeis = diretorio.papeis();
    if (papeis.isEmpty())
      return Map.of();

    var broker = new SessionBroker(motor);
    var res = new LinkedHashMap<String, List<User>>();
    for (Role papel : papeis) {
      var chegam = new ArrayList<User>();
      for (User user : diretorio.usuarios()) {
        var sessao = broker.assumir(user, papel);
        if (sessao != null && motor.isAllowed(sessao, permissao, recurso))
          chegam.add(user);
      }
      if (!chegam.isEmpty())
        res.put(papel.getName(), chegam);
    }
    return res;
  }

  /**
   * Os principais que alguma concessão poderia alcançar, ou {@code null} quando
   * não deu para restringir.
   */
  private List<User> candidatos(Permission permissao, Resource recurso) {
    // o contexto vai sem principal: é justamente o lado que fica em aberto
    var doRecurso = motor.getContexto().resolver(null, recurso);
    var candidatos = new LinkedHashSet<User>();

    for (Group grupo : diretorio.grupos()) {
      for (Statement statement : concessoes(grupo.getStatements(), permissao)) {
        // uma cláusula que mira TURMA/3 não candidata ninguém para a turma 9,
        // e isso se decide sem avaliar condição nenhuma
        if (!statement.getResource().casa(recurso))
          continue;
        var restricao = PrincipalConstraintExtractor.extrair(statement.getCondition(), doRecurso);
        if (restricao.irrestrita())
          candidatos.addAll(grupo.getUsers());
        else
          adicionarPorId(candidatos, grupo.getUsers(), restricao.getIds());
      }
    }

    // concessões inline não estão indexadas por parte nenhuma: percorrer os
    // usuários é inevitável, mas basta olhar as cláusulas, sem avaliar condição
    for (User user : diretorio.usuarios()) {
      var alcancam = concessoes(user.getStatements(), permissao).stream()
          .anyMatch(s -> s.getResource().casa(recurso));
      if (alcancam)
        candidatos.add(user);
    }

    // a política anexada ao próprio recurso também concede, e aqui ela é barata
    // de considerar: o recurso está fixo, então basta ler as cláusulas dele
    if (politicasDeRecurso != null && recurso != null) {
      var doRecursoPolicy = politicasDeRecurso.politicaDe(recurso);
      if (doRecursoPolicy != null) {
        for (Statement statement : concessoes(doRecursoPolicy.getStatements(), permissao)) {
          if (!statement.getResource().casa(recurso))
            continue;
          var restricao = PrincipalConstraintExtractor.extrair(statement.getCondition(), doRecurso);
          if (restricao.irrestrita())
            candidatos.addAll(diretorio.usuarios());
          else
            adicionarPorId(candidatos, new LinkedHashSet<>(diretorio.usuarios()),
                restricao.getIds());
        }
      }
    }

    return new ArrayList<>(candidatos);
  }

  /**
   * O dual: dado um principal e uma ação, o filtro sobre os recursos que ele
   * alcança. Vale a mesma regra — o filtro escolhe candidatos, o motor decide.
   */
  public ResourceConstraint ondePosso(User principal, Permission permissao) {
    var doPrincipal = motor.getContexto().resolver(principal, null);
    var partes = new ArrayList<ResourceConstraint>();

    for (Statement statement : concessoes(principal.getStatements(), permissao))
      partes.add(restricaoDe(statement, doPrincipal));

    for (Group grupo : diretorio.grupos()) {
      if (!grupo.getUsers().contains(principal))
        continue;
      for (Statement statement : concessoes(grupo.getStatements(), permissao))
        partes.add(restricaoDe(statement, doPrincipal));
    }

    var compartilhados = compartilhadosCom(principal, permissao);
    if (compartilhados != null)
      partes.add(compartilhados);

    if (partes.isEmpty())
      return ResourceConstraint.Nada.INSTANCIA;
    // as concessões se somam: alcança o que qualquer uma delas alcançar
    return partes.size() == 1 ? partes.get(0) : new ResourceConstraint.Alguma(partes);
  }

  /**
   * Os recursos que concedem a este principal pela política deles.
   *
   * Aqui não há filtro a derivar: uma concessão que mora no recurso não é uma
   * regra sobre atributos, é uma lista. Por isso a única resposta possível é
   * perguntar ao provedor quais recursos têm política própria e testar cada
   * um — e por isso a enumeração precisou entrar no spi. Devolve {@code null}
   * quando não há nada a acrescentar.
   */
  private ResourceConstraint compartilhadosCom(User principal, Permission permissao) {
    if (politicasDeRecurso == null)
      return null;

    var ids = new LinkedHashSet<String>();
    for (Resource recurso : politicasDeRecurso.comPoliticaPropria(permissao.getResourceType())) {
      var policy = politicasDeRecurso.politicaDe(recurso);
      if (policy == null)
        continue;
      var ctx = motor.getContexto().resolver(principal, recurso);
      for (Statement statement : concessoes(policy.getStatements(), permissao)) {
        if (statement.getResource().casa(recurso) && statement.getCondition().avaliar(ctx)) {
          ids.add(recurso.getId());
          break;
        }
      }
    }
    return ids.isEmpty() ? null : new ResourceConstraint.IdEm(ids);
  }

  /**
   * Aplica o filtro e confirma cada sobrevivente com o motor — é o que impede
   * um erro na extração de virar acesso indevido.
   */
  public <T extends Resource> List<T> filtrar(User principal, Permission permissao,
      Collection<T> candidatos) {
    var filtro = PredicateRenderer.render(ondePosso(principal, permissao), motor.getContexto());
    return candidatos.stream()
        .filter(filtro)
        .filter(r -> motor.isAllowed(principal, permissao, r))
        .toList();
  }

  /**
   * O que uma cláusula alcança: a condição dela, mais o recurso que ela mira.
   *
   * O padrão de recurso é a metade que a condição não tem. Uma cláusula sobre
   * {@code TURMA/3} restringe a uma turma sem escrever condição nenhuma, e
   * ignorá-la aqui não daria resposta errada — daria o filtro dizendo "todas as
   * turmas" e o motor recusando uma por uma depois. Foi para isto que o
   * elemento entrou no {@link Statement}.
   */
  private static ResourceConstraint restricaoDe(Statement statement, RequestContext doPrincipal) {
    var daCondicao = ResourceConstraintExtractor.extrair(statement.getCondition(), doPrincipal);
    if (!statement.getResource().idExato())
      return daCondicao;

    var doPadrao = new ResourceConstraint.AtributoIgual("recurso:id",
        statement.getResource().getId());
    if (daCondicao instanceof ResourceConstraint.Nada)
      return daCondicao;
    if (daCondicao instanceof ResourceConstraint.Tudo)
      return doPadrao;
    // as duas precisam valer: a cláusula mira aquele recurso E sob aquela condição
    return new ResourceConstraint.Todas(List.of(doPadrao, daCondicao));
  }

  private static List<Statement> concessoes(Set<Statement> statements, Permission permissao) {
    return statements.stream()
        .filter(s -> s.getEffect() == Effect.ALLOW && s.falaSobre(permissao))
        .toList();
  }

  private static void adicionarPorId(Set<User> destino, Collection<User> membros, Set<String> ids) {
    for (User membro : membros)
      if (ids.contains(membro.getId()))
        destino.add(membro);
  }
}
