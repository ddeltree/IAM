package poo.iam;

import poo.iam.resources.Resource;

public class PermissionService {
  public static boolean hasPermission(User user, Permission permission) {
    if (user.hasInlinePermission(permission))
      return true;
    for (Group group : user.getGroups()) {
      if (group.hasPermission(permission))
        return true;
    }
    return false;
  }

  public static boolean hasPermission(User user, Action action, Resource resource) {
    var permission = new Permission(action, resource);
    return hasPermission(user, permission);
  }

}
