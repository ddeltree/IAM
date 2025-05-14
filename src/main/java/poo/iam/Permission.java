package poo.iam;

import java.util.Objects;

import poo.iam.resources.Resource;

public class Permission {
  private Action action;
  private Resource resource;

  public Permission(Action action, Resource resource) {
    this.action = action;
    this.resource = resource;
  }

  public Action getAction() {
    return action;
  }

  public Resource getResource() {
    return resource;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o)
      return true;
    if (!(o instanceof Permission))
      return false;
    Permission that = (Permission) o;
    return action == that.action && resource.getType().equals(that.resource.getType());
  }

  @Override
  public int hashCode() {
    return Objects.hash(action, resource.getType());
  }

  @Override
  public String toString() {
    return action + ":" + resource.getType();
  }
}
