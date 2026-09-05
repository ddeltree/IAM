package poo.iam;

import java.util.Objects;

import poo.iam.condition.CondicaoOpaca;
import poo.iam.condition.Condition;

/**
 * Uma cláusula de política: efeito, que ações, sobre que recursos, sob que
 * condição.
 *
 * Os quatro campos são os mesmos da AWS, e nenhum deles é acidente:
 *
 * <ul>
 *   <li>a <b>condição</b> mora aqui, e não na definição da ação, para que a
 *       mesma permissão possa ser concedida com restrições diferentes a
 *       principais diferentes — um grupo recebe EDITAR_POST só para os próprios
 *       posts, o administrador recebe EDITAR_POST sem restrição;</li>
 *   <li>a <b>ação</b> é padrão, e não nome exato, para que "pode tudo" caiba em
 *       uma cláusula em vez de uma por ação;</li>
 *   <li>o <b>recurso</b> é padrão, e não só um tipo, para que se possa mirar
 *       {@code TURMA/3} dizendo o que se quer dizer — e para que as consultas
 *       consigam descartar a cláusula sem avaliar condição nenhuma.</li>
 * </ul>
 */
public class Statement {
  private final Effect effect;
  private final ActionPattern action;
  private final ResourcePattern resource;
  private final Condition condition;
  private final String sid;

  private Statement(Effect effect, ActionPattern action, ResourcePattern resource,
      Condition condition, String sid) {
    this.effect = effect;
    this.action = action;
    this.resource = resource;
    this.condition = condition;
    this.sid = sid != null ? sid : effect + ":" + action + ":" + resource;
  }

  // ---------- a partir de uma permissão concreta ----------

  public static Statement allow(Permission permission) {
    return allow(permission, Condition.SEMPRE);
  }

  public static Statement allow(Permission permission, Condition condition) {
    return new Statement(Effect.ALLOW, ActionPattern.de(permission.getAction()),
        ResourcePattern.deTipo(permission.getResourceType()), condition, null);
  }

  /** Ponte para uma condição ainda escrita em código. Veja {@link CondicaoOpaca}. */
  public static Statement allow(Permission permission, PermissionCondition legado) {
    return allow(permission, new CondicaoOpaca(legado));
  }

  public static Statement deny(Permission permission) {
    return deny(permission, Condition.SEMPRE);
  }

  public static Statement deny(Permission permission, Condition condition) {
    return new Statement(Effect.DENY, ActionPattern.de(permission.getAction()),
        ResourcePattern.deTipo(permission.getResourceType()), condition, null);
  }

  public static Statement deny(Permission permission, PermissionCondition legado) {
    return deny(permission, new CondicaoOpaca(legado));
  }

  // ---------- a partir de padrões ----------

  /** {@code allow("*", "*")} — o administrador, em uma linha. */
  public static Statement allow(String acao, String recurso) {
    return de(Effect.ALLOW, acao, recurso, Condition.SEMPRE);
  }

  public static Statement de(Effect effect, String acao, String recurso, Condition condition) {
    return new Statement(effect, ActionPattern.de(acao), ResourcePattern.de(recurso),
        condition, null);
  }

  public static Statement de(Effect effect, ActionPattern acao, ResourcePattern recurso,
      Condition condition, String sid) {
    return new Statement(effect, acao, recurso, condition, sid);
  }

  /** A mesma cláusula, com o nome que o autor quis dar. */
  public Statement comSid(String sid) {
    return new Statement(effect, action, resource, condition, sid);
  }

  // ---------- casamento ----------

  /**
   * A cláusula fala sobre este pedido e a condição dela passa?
   *
   * O recurso vem do contexto porque ele já está ali: montá-lo é a parte cara,
   * e o motor o faz uma vez para todas as cláusulas.
   */
  public boolean aplica(Permission permission, RequestContext ctx) {
    return falaSobre(permission) && resource.casa(ctx.getRecurso()) && condition.avaliar(ctx);
  }

  /** Fala sobre esta permissão, ignorando condição e instância? */
  public boolean falaSobre(Permission permission) {
    return action.casa(permission.getAction()) && resource.casaTipo(permission.getResourceType());
  }

  /**
   * Nome da cláusula, como o {@code Sid} da AWS. Serve para dizer qual delas
   * decidiu um pedido; quando o autor não dá um, é gerado a partir do conteúdo.
   */
  public String getSid() {
    return sid;
  }

  public Effect getEffect() {
    return effect;
  }

  public ActionPattern getAction() {
    return action;
  }

  public ResourcePattern getResource() {
    return resource;
  }

  /**
   * A permissão exata desta cláusula, ou {@code null} quando ela usa curinga.
   *
   * Uma cláusula com curinga não corresponde a uma permissão — corresponde a
   * um conjunto delas, e enumerar esse conjunto exige saber o que existe. Quem
   * responde isso é {@link EffectivePermissions}, com o catálogo em mãos.
   */
  public Permission getPermission() {
    var tipo = resource.getTipo();
    if (!action.exato() || tipo.indexOf('*') >= 0 || tipo.indexOf('?') >= 0)
      return null;
    return new Permission(new ActionNomeada(action.getPadrao()), new ResourceTypeNomeado(tipo));
  }

  public Condition getCondition() {
    return condition;
  }

  /** Uma ação identificada só pelo nome, para reconstruir a partir do padrão. */
  private record ActionNomeada(String name) implements Action {
  }

  private record ResourceTypeNomeado(String name) implements ResourceType {
  }

  /**
   * Duas cláusulas são iguais quando dizem a mesma coisa. O {@code sid} fica de
   * fora: ele é rótulo, não conteúdo — como na AWS.
   */
  @Override
  public boolean equals(Object o) {
    if (this == o)
      return true;
    if (!(o instanceof Statement))
      return false;
    var that = (Statement) o;
    return effect == that.effect
        && action.equals(that.action)
        && resource.equals(that.resource)
        && condition.equals(that.condition);
  }

  @Override
  public int hashCode() {
    return Objects.hash(effect, action, resource, condition);
  }

  @Override
  public String toString() {
    var restrita = condition != Condition.SEMPRE ? " (condicional)" : "";
    return sid + restrita;
  }
}
