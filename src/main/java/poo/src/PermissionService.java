package poo.src;

import poo.src.resources.Resource;

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

  public static boolean hasPermission(User user, Resource resource, Action action) {
    var permission = new Permission(action, resource.getType());
    return hasPermission(user, permission);
  }
}
