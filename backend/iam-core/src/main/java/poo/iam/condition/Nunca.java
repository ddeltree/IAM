package poo.iam.condition;

import java.util.Set;

import poo.iam.RequestContext;

/**
 * A condição que nunca passa.
 *
 * Numa cláusula ALLOW ela a desliga sem apagá-la — o equivalente a comentar uma
 * linha da política, mas de um jeito que sobrevive à serialização e continua
 * aparecendo no documento. É também o que a extração de restrição devolve
 * quando descobre, sem avaliar nada, que uma cláusula não alcança ninguém.
 */
public final class Nunca implements Condition {

  public static final Nunca INSTANCIA = new Nunca();

  private Nunca() {
  }

  @Override
  public boolean avaliar(RequestContext ctx) {
    return false;
  }

  @Override
  public Set<String> chaves() {
    return Set.of();
  }

  @Override
  public <R> R accept(ConditionVisitor<R> visitor) {
    return visitor.visitarNunca(this);
  }

  @Override
  public String toString() {
    return "nunca";
  }
}
