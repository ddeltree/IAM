package poo.iam;

import java.util.*;

import com.fasterxml.jackson.annotation.JsonIgnore;

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

  /**
   * Nega a permissão para este usuário, sobrepondo o que os grupos dele
   * concedem.
   */
  public boolean denyPermission(Permission permission) {
    var res = permissionHolder.deny(permission);
    if (res)
      System.out.println("[" + id + "] " + "User permission DENIED: " + permission);
    return res;
  }

  /**
   * Remove a negação explícita. Não concede nada por si só — o usuário volta a
   * depender das permissões inline e das herdadas dos grupos.
   */
  public boolean allowPermission(Permission permission) {
    var res = permissionHolder.allow(permission);
    if (res)
      System.out.println("[" + id + "] " + "User permission deny removed: " + permission);
    return res;
  }

  protected boolean hasInlinePermission(Permission permission) {
    return permissionHolder.has(permission);
  }

  protected boolean hasInlineDeny(Permission permission) {
    return permissionHolder.isDenied(permission);
  }

  @JsonIgnore
  public Set<Permission> getInlinePermissions() {
    return permissionHolder.getPermissions();
  }

  @JsonIgnore
  public Set<Permission> getDeniedPermissions() {
    return permissionHolder.getDeniedPermissions();
  }

  /** Esvazia as permissões inline (concedidas e negadas). */
  protected void clearPermissions() {
    permissionHolder.clear();
  }

  // GROUPS

  protected void joinGroup(Group group) {
    groups.add(group);
  }

  protected void quitGroup(Group group) {
    groups.remove(group);
  }

  /**
   * Grupos e permissões ficam fora do JSON: são detalhe interno da
   * autorização e apareciam em toda resposta que embute um usuário (o autor de
   * um post, por exemplo). Quem precisa do papel usa o DTO do UserController.
   */
  @JsonIgnore
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

  /**
   * Reinicia o contador de ids. Existe para os testes conseguirem rodar cada
   * cenário a partir de um estado previsível.
   */
  public static void resetIdCounter(long proximo) {
    proximoId = proximo;
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
