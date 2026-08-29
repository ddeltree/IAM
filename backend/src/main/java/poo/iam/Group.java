package poo.iam;

import java.util.*;

import poo.iam.condition.Condition;

public class Group {
  private final String name;
  private final Set<User> users = new HashSet<>();
  private final PermissionHolder policy = new PermissionHolder(); // composition

  public Group(String name) {
    this.name = name;
  }

  protected void addUser(User user) {
    users.add(user);
  }

  protected void removeUser(User user) {
    users.remove(user);
  }

  /** Remove todos os membros. */
  public void clearUsers() {
    users.clear();
  }

  public String getName() {
    return name;
  }

  // PERMISSIONS

  /** Concessão irrestrita para todos os membros. */
  public boolean grantPermission(Permission permission) {
    return registrar(policy.grant(permission), "GRANTED", permission);
  }

  /** Concessão válida só quando a condição passar. */
  public boolean grantPermission(Permission permission, Condition condition) {
    return registrar(policy.grant(permission, condition), "GRANTED", permission);
  }

  public boolean revokePermission(Permission permission) {
    return registrar(policy.revoke(permission), "REVOKED", permission);
  }

  /** Nega a permissão para todos os membros do grupo. */
  public boolean denyPermission(Permission permission) {
    return registrar(policy.deny(permission), "DENIED", permission);
  }

  public boolean denyPermission(Permission permission, Condition condition) {
    return registrar(policy.deny(permission, condition), "DENIED", permission);
  }

  /** Remove a negação explícita do grupo. Não concede a permissão. */
  public boolean allowPermission(Permission permission) {
    return registrar(policy.allow(permission), "deny removed", permission);
  }

  private boolean registrar(boolean mudou, String verbo, Permission permission) {
    if (mudou)
      System.out.println("(" + name + ") Group permission " + verbo + ": " + permission);
    return mudou;
  }

  PermissionHolder getPolicy() {
    return policy;
  }

  public boolean hasPermission(Permission permission) {
    return policy.has(permission);
  }

  public boolean deniesPermission(Permission permission) {
    return policy.isDenied(permission);
  }

  /** As cláusulas do grupo, para inspeção. */
  public Set<Statement> getStatements() {
    return policy.getStatements();
  }

  public Set<Permission> getPermissions() {
    return policy.getPermissions();
  }

  public Set<Permission> getDeniedPermissions() {
    return policy.getDeniedPermissions();
  }

  /** Esvazia a política do grupo. */
  public void clearPermissions() {
    policy.clear();
  }
}
