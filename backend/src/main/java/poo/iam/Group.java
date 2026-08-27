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

  protected void clearUsers() {
    users.clear();
  }

  public String getName() {
    return name;
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

  /** Nega a permissão para todos os membros do grupo. */
  public boolean denyPermission(Permission permission) {
    var res = permissionHolder.deny(permission);
    if (res)
      System.out.println("(" + name + ") " + "Group permission DENIED: " + permission);
    return res;
  }

  /** Remove a negação explícita do grupo. Não concede a permissão. */
  public boolean allowPermission(Permission permission) {
    var res = permissionHolder.allow(permission);
    if (res)
      System.out.println("(" + name + ") " + "Group permission deny removed: " + permission);
    return res;
  }

  public boolean hasPermission(Permission permission) {
    return permissionHolder.has(permission);
  }

  public boolean deniesPermission(Permission permission) {
    return permissionHolder.isDenied(permission);
  }

  public Set<Permission> getPermissions() {
    return permissionHolder.getPermissions();
  }

  public Set<Permission> getDeniedPermissions() {
    return permissionHolder.getDeniedPermissions();
  }

  /** Esvazia as permissões do grupo (concedidas e negadas). */
  protected void clearPermissions() {
    permissionHolder.clear();
  }
}
