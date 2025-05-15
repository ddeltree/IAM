package poo.iam;

import java.util.*;

public class User {
  private String id;
  private String name;
  private final Set<Group> groups = new HashSet<>();
  private final PermissionHolder permissionHolder = new PermissionHolder(); // composition

  public User() {
    this.id = UUID.randomUUID().toString();
  }

  public User(String id) {
    this.id = id;
  }

  public User(String id, String name) {
    this(id);
    this.name = name;
  }

  // PERMISSIONS

  public boolean grantPermission(Permission permission) {
    var res = permissionHolder.grant(permission);
    if (res)
      System.out.println("[" + id + "] " + "User permission granted: " + permission);
    return res;
  }

  public boolean revokePermission(Permission permission) {
    var res = permissionHolder.revoke(permission);
    if (res)
      System.out.println("[" + id + "] " + "User permission revoked: " + permission);
    return res;
  }

  protected boolean hasInlinePermission(Permission permission) {
    return permissionHolder.has(permission);
  }

  public Set<Permission> getInlinePermissions() {
    return permissionHolder.getPermissions();
  }

  // GROUPS

  protected void joinGroup(Group group) {
    groups.add(group);
  }

  protected void quitGroup(Group group) {
    groups.remove(group);
  }

  public Set<Group> getGroups() {
    return Collections.unmodifiableSet(groups);
  }

  // GETTERS

  public String getId() {
    return id;
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }
}
