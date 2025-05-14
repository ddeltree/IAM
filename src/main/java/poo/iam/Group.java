package poo.iam;

import java.util.*;

public class Group {
  private String name;
  private Set<User> users;
  private final PermissionHolder permissionHolder = new PermissionHolder(); // composition

  public Group(String name) {
    this.name = name;
    this.users = new HashSet<>();
  }

  protected void addUser(User user) {
    users.add(user);
  }

  protected void removeUser(User user) {
    users.remove(user);
  }

  // PERMISSIONS

  public boolean grantPermission(Permission permission) {
    var res = permissionHolder.grant(permission);
    if (res)
      System.out.println("(" + name + ") " + "Group permission GRANTED: " + permission);
    return res;
  }

  public boolean revokePermission(Permission permission) {
    var res = permissionHolder.revoke(permission);
    if (res)
      System.out.println("(" + name + ") " + "Group permission REVOKED: " + permission);
    return res;
  }

  public boolean hasPermission(Permission permission) {
    return permissionHolder.has(permission);
  }

  public Set<Permission> getPermissions() {
    return permissionHolder.getPermissions();
  }
}
