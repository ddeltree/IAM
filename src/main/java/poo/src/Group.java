package poo.src;

import java.util.*;

public class Group {
  private String name;
  private Set<User> users;
  private Set<Permission> groupPermissions;

  public Group(String name) {
    this.name = name;
    this.users = new HashSet<>();
    this.groupPermissions = new HashSet<>();
  }

  protected void addUser(User user) {
    users.add(user);
  }

  protected void removeUser(User user) {
    users.remove(user);
  }

  public void assignPermission(Permission permission) {
    groupPermissions.add(permission);
    for (User user : users) {
      user.assignPermission(permission);
    }
  }

  public boolean hasPermission(Permission permission) {
    return groupPermissions.contains(permission);
  }

  // getters and setters
}
