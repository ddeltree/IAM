package poo.iam.condition;

import java.util.Set;

import poo.iam.PermissionCondition;
import poo.iam.RequestContext;

/**
 * Uma condição que ainda é código, embrulhada para caber na árvore.
 *
 * Existe para a migração: as condições viram dado uma de cada vez, e enquanto
 * uma delas não virou, ela continua avaliando normalmente. O que ela não faz é
 * mentir para quem inspeciona — qualquer visitante recebe "não sei" em vez de
 * um resultado inventado.
 */
public final class CondicaoOpaca implements Condition {

  private final PermissionCondition legado;

  public CondicaoOpaca(PermissionCondition legado) {
    this.legado = legado;
  }

  @Override
  public boolean avaliar(RequestContext ctx) {
    return legado.test(ctx);
  }

  @Override
  public Set<String> chaves() {
    return Set.of();
  }

  @Override
  public <R> R accept(ConditionVisitor<R> visitor) {
    return visitor.visitarOpaca(this);
  }

  @Override
  public String toString() {
    return "condição opaca (ainda em código)";
  }
}
