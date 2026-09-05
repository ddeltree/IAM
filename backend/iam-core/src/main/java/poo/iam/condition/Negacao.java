package poo.iam.condition;

import java.util.Objects;
import java.util.Set;

import poo.iam.RequestContext;

/**
 * Inverte a condição. Também não existe na AWS, onde se usa o operador negado
 * ({@code StringNotEquals}) — mantida junto com {@link AlgumaDas} para a
 * árvore booleana ficar completa.
 */
public final class Negacao implements Condition {

  private final Condition condicao;

  public Negacao(Condition condicao) {
    this.condicao = condicao;
  }

  @Override
  public boolean avaliar(RequestContext ctx) {
    return !condicao.avaliar(ctx);
  }

  @Override
  public Set<String> chaves() {
    return condicao.chaves();
  }

  @Override
  public <R> R accept(ConditionVisitor<R> visitor) {
    return visitor.visitarNegacao(this);
  }

  public Condition getCondicao() {
    return condicao;
  }

  @Override
  public boolean equals(Object o) {
    return o instanceof Negacao && condicao.equals(((Negacao) o).condicao);
  }

  @Override
  public int hashCode() {
    return Objects.hash("Negacao", condicao);
  }

  @Override
  public String toString() {
    return "nao(" + condicao + ")";
  }
}
