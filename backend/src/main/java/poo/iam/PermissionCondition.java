package poo.iam;

import poo.iam.resources.Resource;

@FunctionalInterface
public interface PermissionCondition {
  boolean test(User user, Resource resource, Object... context);
}