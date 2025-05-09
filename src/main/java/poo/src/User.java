package poo.src;

import java.util.*;

public class User {
  private String id;
  private final Set<Permission> permissions = new HashSet<>();
  private final Set<Permission> deniedPermissions = new HashSet<>();
  private final Set<Group> groups = new HashSet<>();

  public User(String id) {
    this.id = id;
  }

  // PERMISSIONS

  public void assignPermission(Permission permission) {
    permissions.add(permission);
  }

  public void denyPermission(Permission permission) {
    deniedPermissions.add(permission);
  }

  public void revokePermission(Permission permission) {
    permissions.remove(permission);
    deniedPermissions.remove(permission);
  }

  public boolean hasPermission(Permission permission) {
    if (deniedPermissions.contains(permission))
      return false;
    if (permissions.contains(permission))
      return true;
    for (Group group : groups) {
      if (group.hasPermission(permission))
        return true;
    }
    return false;
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
}
