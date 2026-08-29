package poo.iam;

import java.util.Objects;

/** O par (ação, tipo de recurso) — o "o quê" de uma autorização. */
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
    return action.equals(that.action) && resourceType.equals(that.resourceType);
  }

  @Override
  public int hashCode() {
    return Objects.hash(action, resourceType);
  }

  @Override
  public String toString() {
    return action.name() + ":" + resourceType.name();
  }
}
