package poo.iam;

import java.util.*;

import poo.iam.resources.Resource;
import poo.iam.resources.ResourceTypes;

public class User implements Resource {
  private static long proximoId = 1; // contador global
  protected final String id = String.valueOf(proximoId++);
  private String name;
  private final Set<Group> groups = new HashSet<>();
  private final PermissionHolder permissionHolder = new PermissionHolder(); // composition

  public User() {
  }

  public User(String name) {
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

  @Override
  public boolean equals(Object o) {
    if (this == o)
      return true;
    if (!(o instanceof User))
      return false;
    var other = (User) o;
    return id.equals(other.getId());
  }

  @Override
  public int hashCode() {
    return id.hashCode();
  }

  @Override
  public ResourceTypes getType() {
    return ResourceTypes.USUARIO;
  }
}
