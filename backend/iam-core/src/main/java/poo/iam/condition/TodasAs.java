package poo.iam.condition;

import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

import poo.iam.RequestContext;

/** Todas as condições precisam passar — o E implícito do bloco da AWS. */
public final class TodasAs implements Condition {

  private final List<Condition> condicoes;

  public TodasAs(List<Condition> condicoes) {
    this.condicoes = List.copyOf(condicoes);
  }

  @Override
  public boolean avaliar(RequestContext ctx) {
    return condicoes.stream().allMatch(c -> c.avaliar(ctx));
  }

  @Override
  public Set<String> chaves() {
    var res = new HashSet<String>();
    condicoes.forEach(c -> res.addAll(c.chaves()));
    return res;
  }

  @Override
  public <R> R accept(ConditionVisitor<R> visitor) {
    return visitor.visitarTodasAs(this);
  }

  public List<Condition> getCondicoes() {
    return condicoes;
  }

  @Override
  public boolean equals(Object o) {
    return o instanceof TodasAs && condicoes.equals(((TodasAs) o).condicoes);
  }

  @Override
  public int hashCode() {
    return Objects.hash("TodasAs", condicoes);
  }

  @Override
  public String toString() {
    return "todasAs" + condicoes;
  }
}
