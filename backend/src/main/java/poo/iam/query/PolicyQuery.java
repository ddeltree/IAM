package poo.iam.query;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import poo.iam.AccessResolver;
import poo.iam.ContextResolver;
import poo.iam.Effect;
import poo.iam.Group;
import poo.iam.Permission;
import poo.iam.PrincipalDirectory;
import poo.iam.Resource;
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

  public PolicyQuery(PrincipalDirectory diretorio) {
    this.diretorio = diretorio;
  }

  /** O resultado, com o rastro de quanto trabalho foi evitado. */
  public static final class Resultado {
    public final List<User> principais;
    public final int avaliados;
    public final int conhecidos;
    public final boolean podou;

    Resultado(List<User> principais, int avaliados, int conhecidos, boolean podou) {
      this.principais = principais;
      this.avaliados = avaliados;
      this.conhecidos = conhecidos;
      this.podou = podou;
    }
  }

  /** Implementação de referência: pergunta ao motor sobre todo mundo. */
  public List<User> quemPodeVarrendo(Permission permissao, Resource recurso) {
    return diretorio.usuarios().stream()
        .filter(u -> AccessResolver.isAllowed(u, permissao, recurso))
        .toList();
  }

  public Resultado quemPode(Permission permissao, Resource recurso) {
    var candidatos = candidatos(permissao, recurso);
    var conhecidos = diretorio.usuarios().size();

    // sem poda possível, a lista de candidatos é todo mundo
    var aAvaliar = candidatos == null ? new ArrayList<>(diretorio.usuarios()) : candidatos;

    var permitidos = aAvaliar.stream()
        .filter(u -> AccessResolver.isAllowed(u, permissao, recurso))
        .toList();

    return new Resultado(permitidos, aAvaliar.size(), conhecidos, candidatos != null);
  }

  /**
   * Os principais que alguma concessão poderia alcançar, ou {@code null} quando
   * não deu para restringir.
   */
  private List<User> candidatos(Permission permissao, Resource recurso) {
    // o contexto vai sem principal: é justamente o lado que fica em aberto
    var doRecurso = ContextResolver.padrao().resolver(null, recurso);
    var candidatos = new LinkedHashSet<User>();

    for (Group grupo : diretorio.grupos()) {
      for (Statement statement : concessoes(grupo.getStatements(), permissao)) {
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
      if (!concessoes(user.getStatements(), permissao).isEmpty())
        candidatos.add(user);
    }

    return new ArrayList<>(candidatos);
  }

  /**
   * O dual: dado um principal e uma ação, o filtro sobre os recursos que ele
   * alcança. Vale a mesma regra — o filtro escolhe candidatos, o motor decide.
   */
  public ResourceConstraint ondePosso(User principal, Permission permissao) {
    var doPrincipal = ContextResolver.padrao().resolver(principal, null);
    var partes = new ArrayList<ResourceConstraint>();

    for (Statement statement : concessoes(principal.getStatements(), permissao))
      partes.add(ResourceConstraintExtractor.extrair(statement.getCondition(), doPrincipal));

    for (Group grupo : diretorio.grupos()) {
      if (!grupo.getUsers().contains(principal))
        continue;
      for (Statement statement : concessoes(grupo.getStatements(), permissao))
        partes.add(ResourceConstraintExtractor.extrair(statement.getCondition(), doPrincipal));
    }

    if (partes.isEmpty())
      return ResourceConstraint.Nada.INSTANCIA;
    // as concessões se somam: alcança o que qualquer uma delas alcançar
    return partes.size() == 1 ? partes.get(0) : new ResourceConstraint.Alguma(partes);
  }

  /**
   * Aplica o filtro e confirma cada sobrevivente com o motor — é o que impede
   * um erro na extração de virar acesso indevido.
   */
  public <T extends Resource> List<T> filtrar(User principal, Permission permissao,
      Collection<T> candidatos) {
    var filtro = PredicateRenderer.render(ondePosso(principal, permissao));
    return candidatos.stream()
        .filter(filtro)
        .filter(r -> AccessResolver.isAllowed(principal, permissao, r))
        .toList();
  }

  private static List<Statement> concessoes(Set<Statement> statements, Permission permissao) {
    return statements.stream()
        .filter(s -> s.getEffect() == Effect.ALLOW && s.falaSobre(permissao))
        .toList();
  }

  private static void adicionarPorId(Set<User> destino, Set<User> membros, Set<String> ids) {
    for (User membro : membros)
      if (ids.contains(membro.getId()))
        destino.add(membro);
  }
}
