package poo.iam;

import java.util.Objects;

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
  private final PermissionCondition condition;
  private final String sid;

  private Statement(Effect effect, Permission permission, PermissionCondition condition) {
    this.effect = effect;
    this.permission = permission;
    this.condition = condition;
    this.sid = effect + ":" + permission;
  }

  public static Statement allow(Permission permission) {
    return new Statement(Effect.ALLOW, permission, PermissionCondition.SEMPRE);
  }

  public static Statement allow(Permission permission, PermissionCondition condition) {
    return new Statement(Effect.ALLOW, permission, condition);
  }

  public static Statement deny(Permission permission) {
    return new Statement(Effect.DENY, permission, PermissionCondition.SEMPRE);
  }

  public static Statement deny(Permission permission, PermissionCondition condition) {
    return new Statement(Effect.DENY, permission, condition);
  }

  /** A cláusula fala sobre esta permissão e a condição dela passa? */
  public boolean aplica(Permission permission, User user, Resource resource, Object... context) {
    return this.permission.equals(permission) && condition.test(user, resource, context);
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

  public PermissionCondition getCondition() {
    return condition;
  }

  /**
   * Duas cláusulas são iguais quando têm o mesmo efeito, a mesma permissão e a
   * mesma condição. Como as condições costumam ser lambdas, a comparação delas
   * é por identidade — o suficiente para não duplicar a mesma concessão.
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
        && condition == that.condition;
  }

  @Override
  public int hashCode() {
    return Objects.hash(effect, permission, System.identityHashCode(condition));
  }

  @Override
  public String toString() {
    var restrita = condition != PermissionCondition.SEMPRE ? " (condicional)" : "";
    return sid + restrita;
  }
}
