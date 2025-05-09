package poo.src;

import java.util.*;

// TODO adicionar a lógica de negar permissões
public class PermissionHolder {
  private final Set<Permission> permissions = new HashSet<>();

  public boolean grant(Permission permission) {
    return permissions.add(permission);
  }

  public boolean revoke(Permission permission) {
    return permissions.remove(permission);
  }

  public boolean has(Permission permission) {
    return permissions.contains(permission);
  }

  public Set<Permission> getPermissions() {
    return Collections.unmodifiableSet(permissions);
  }
}
