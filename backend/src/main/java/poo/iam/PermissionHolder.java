package poo.iam;

import java.util.*;

/**
 * Conjunto de permissões de um {@link User} ou {@link Group}.
 *
 * Guarda duas listas independentes: as permissões concedidas e as negadas
 * explicitamente. A negação nunca é apagada por uma concessão — quem resolve o
 * conflito é {@link SystemPermission}, que aplica a regra "deny overrides":
 * basta uma negação (no usuário ou em qualquer grupo dele) para o acesso cair.
 */
public class PermissionHolder {
  private final Set<Permission> permissions = new HashSet<>();
  private final Set<Permission> deniedPermissions = new HashSet<>();

  public boolean grant(Permission permission) {
    return permissions.add(permission);
  }

  public boolean revoke(Permission permission) {
    return permissions.remove(permission);
  }

  public boolean has(Permission permission) {
    return permissions.contains(permission);
  }

  /** Nega a permissão explicitamente, sobrepondo qualquer concessão. */
  public boolean deny(Permission permission) {
    return deniedPermissions.add(permission);
  }

  /**
   * Remove a negação explícita. Não concede a permissão: se ela não estiver
   * concedida aqui nem em um grupo, o acesso continua barrado.
   */
  public boolean allow(Permission permission) {
    return deniedPermissions.remove(permission);
  }

  public boolean isDenied(Permission permission) {
    return deniedPermissions.contains(permission);
  }

  public Set<Permission> getPermissions() {
    return Collections.unmodifiableSet(permissions);
  }

  public Set<Permission> getDeniedPermissions() {
    return Collections.unmodifiableSet(deniedPermissions);
  }

  /** Esvazia concessões e negações. */
  public void clear() {
    permissions.clear();
    deniedPermissions.clear();
  }
}
