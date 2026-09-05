package poo.iam;

import java.util.Objects;

import poo.iam.condition.CondicaoOpaca;
import poo.iam.condition.Condition;

/**
 * Uma cláusula de política: efeito + permissão + condição.
 *
 * A condição mora aqui, e não na definição da ação, justamente para que a mesma
 * permissão possa ser concedida com restrições diferentes a principais
 * diferentes — um grupo recebe EDITAR_POST só para os próprios posts, o
 * administrador recebe EDITAR_POST sem restrição.
 */
public class Statement {
  private final Effect effect;
  private final Permission permission;
  private final Condition condition;
  private final String sid;

  private Statement(Effect effect, Permission permission, Condition condition) {
    this.effect = effect;
    this.permission = permission;
    this.condition = condition;
    this.sid = effect + ":" + permission;
  }

  public static Statement allow(Permission permission) {
    return new Statement(Effect.ALLOW, permission, Condition.SEMPRE);
  }

  public static Statement allow(Permission permission, Condition condition) {
    return new Statement(Effect.ALLOW, permission, condition);
  }

  /** Ponte para uma condição ainda escrita em código. Veja {@link CondicaoOpaca}. */
  public static Statement allow(Permission permission, PermissionCondition legado) {
    return new Statement(Effect.ALLOW, permission, new CondicaoOpaca(legado));
  }

  public static Statement deny(Permission permission) {
    return new Statement(Effect.DENY, permission, Condition.SEMPRE);
  }

  public static Statement deny(Permission permission, Condition condition) {
    return new Statement(Effect.DENY, permission, condition);
  }

  public static Statement deny(Permission permission, PermissionCondition legado) {
    return new Statement(Effect.DENY, permission, new CondicaoOpaca(legado));
  }

  /** A cláusula fala sobre esta permissão e a condição dela passa? */
  public boolean aplica(Permission permission, RequestContext ctx) {
    return this.permission.equals(permission) && condition.avaliar(ctx);
  }

  public boolean falaSobre(Permission permission) {
    return this.permission.equals(permission);
  }

  /**
   * Nome da cláusula, como o {@code Sid} da AWS. Serve para dizer qual delas
   * decidiu um pedido; duas cláusulas que só diferem na condição compartilham o
   * nome, e quem as separa é o principal a que estão anexadas.
   */
  public String getSid() {
    return sid;
  }

  public Effect getEffect() {
    return effect;
  }

  public Permission getPermission() {
    return permission;
  }

  public Condition getCondition() {
    return condition;
  }

  /**
   * Duas cláusulas são iguais quando têm o mesmo efeito, a mesma permissão e a
   * mesma condição. Agora que a condição é dado, a comparação dela é
   * estrutural — duas concessões escritas igual são a mesma concessão.
   */
  @Override
  public boolean equals(Object o) {
    if (this == o)
      return true;
    if (!(o instanceof Statement))
      return false;
    var that = (Statement) o;
    return effect == that.effect
        && permission.equals(that.permission)
        && condition.equals(that.condition);
  }

  @Override
  public int hashCode() {
    return Objects.hash(effect, permission, condition);
  }

  @Override
  public String toString() {
    var restrita = condition != Condition.SEMPRE ? " (condicional)" : "";
    return sid + restrita;
  }
}
