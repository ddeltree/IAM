package poo.iam;

import java.util.Objects;

/**
 * O par (ação, tipo de recurso) — o "o quê" de uma autorização.
 *
 * A comparação é <b>pelo nome</b>, e não pelo objeto. {@link Action} e
 * {@link ResourceType} são vocabulário aberto: o que identifica uma ação é o
 * texto {@code "EDITAR_POST"}, exatamente como na AWS o que identifica uma é
 * {@code "s3:GetObject"}. Comparar por identidade faria a mesma ação declarada
 * em dois lugares — ou reconstruída a partir de um padrão de política — deixar
 * de ser ela mesma.
 */
public class Permission {
  private final Action action;
  private final ResourceType resourceType;

  public Permission(Action action, ResourceType resourceType) {
    this.action = action;
    this.resourceType = resourceType;
  }

  public Permission(Action action, Resource resource) {
    this(action, resource.getType());
  }

  public Action getAction() {
    return action;
  }

  public ResourceType getResourceType() {
    return resourceType;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o)
      return true;
    if (!(o instanceof Permission))
      return false;
    Permission that = (Permission) o;
    return action.name().equals(that.action.name())
        && resourceType.name().equals(that.resourceType.name());
  }

  @Override
  public int hashCode() {
    return Objects.hash(action.name(), resourceType.name());
  }

  @Override
  public String toString() {
    return action.name() + ":" + resourceType.name();
  }
}
