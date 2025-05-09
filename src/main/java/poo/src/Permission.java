package poo.src;

import java.util.Objects;

import poo.src.resources.ResourceTypes;

public class Permission {
  private Action action;
  private ResourceTypes resourceType;

  public Permission(Action action, ResourceTypes resourceType) {
    this.action = action;
    this.resourceType = resourceType;
  }

  public Action getAction() {
    return action;
  }

  public ResourceTypes getResourceType() {
    return resourceType;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o)
      return true;
    if (!(o instanceof Permission))
      return false;
    Permission that = (Permission) o;
    return action == that.action && resourceType.equals(that.resourceType);
  }

  @Override
  public int hashCode() {
    return Objects.hash(action, resourceType);
  }

  @Override
  public String toString() {
    return action + ":" + resourceType;
  }
}
