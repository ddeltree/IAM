package poo.iam.condition;

import java.util.Set;

import poo.iam.RequestContext;

/** Concessão irrestrita. */
public final class Sempre implements Condition {

  static final Sempre INSTANCIA = new Sempre();

  private Sempre() {
  }

  @Override
  public boolean avaliar(RequestContext ctx) {
    return true;
  }

  @Override
  public Set<String> chaves() {
    return Set.of();
  }

  @Override
  public <R> R accept(ConditionVisitor<R> visitor) {
    return visitor.visitarSempre(this);
  }

  @Override
  public String toString() {
    return "sempre";
  }
}
