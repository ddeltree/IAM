package poo.iam.condition;

import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

import poo.iam.RequestContext;

/**
 * Basta uma passar. Não existe na AWS, onde a alternativa se escreve como duas
 * cláusulas — é extensão deliberada, para a árvore booleana ficar completa.
 */
public final class AlgumaDas implements Condition {

  private final List<Condition> condicoes;

  public AlgumaDas(List<Condition> condicoes) {
    this.condicoes = List.copyOf(condicoes);
  }

  @Override
  public boolean avaliar(RequestContext ctx) {
    return condicoes.stream().anyMatch(c -> c.avaliar(ctx));
  }

  @Override
  public Set<String> chaves() {
    var res = new HashSet<String>();
    condicoes.forEach(c -> res.addAll(c.chaves()));
    return res;
  }

  @Override
  public <R> R accept(ConditionVisitor<R> visitor) {
    return visitor.visitarAlgumaDas(this);
  }

  public List<Condition> getCondicoes() {
    return condicoes;
  }

  @Override
  public boolean equals(Object o) {
    return o instanceof AlgumaDas && condicoes.equals(((AlgumaDas) o).condicoes);
  }

  @Override
  public int hashCode() {
    return Objects.hash("AlgumaDas", condicoes);
  }

  @Override
  public String toString() {
    return "algumaDas" + condicoes;
  }
}
