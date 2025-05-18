package poo.iam;

import java.util.Objects;

import poo.iam.resources.Resource;
import poo.iam.resources.ResourceTypes;

public class Permission {
  private Action action;
  private ResourceTypes resource;

  public Permission(Action action, ResourceTypes resourceType) {
    this.action = action;
    this.resource = resourceType;
  }

  public Permission(Action action, Resource resource) {
    this.action = action;
    this.resource = resource.getType();
  }

  public Action getAction() {
    return action;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o)
      return true;
    if (!(o instanceof Permission))
      return false;
    Permission that = (Permission) o;
    return action == that.action && resource == that.resource;
  }

  @Override
  public int hashCode() {
    return Objects.hash(action, resource);
  }

  @Override
  public String toString() {
    return action + ":" + resource;
  }
}
