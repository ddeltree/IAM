package poo.iam;

import java.util.*;

import poo.iam.condition.Condition;

import com.fasterxml.jackson.annotation.JsonIgnore;

public class User implements Resource {
  private static long proximoId = 1; // contador global
  protected final String id = String.valueOf(proximoId++);
  private String name;
  private final Set<Group> groups = new HashSet<>();
  private final PermissionHolder policy = new PermissionHolder(); // composition

  public User() {
  }

  public User(String name) {
    this.name = name;
  }

  // PERMISSIONS

  /** Concessão irrestrita. */
  public boolean grantPermission(Permission permission) {
    return registrar(policy.grant(permission), "granted", permission);
  }

  /** Concessão válida só quando a condição passar. */
  public boolean grantPermission(Permission permission, Condition condition) {
    return registrar(policy.grant(permission, condition), "granted", permission);
  }

  public boolean revokePermission(Permission permission) {
    return registrar(policy.revoke(permission), "revoked", permission);
  }

  /**
   * Nega a permissão para este usuário, sobrepondo o que os grupos dele
   * concedem.
   */
  public boolean denyPermission(Permission permission) {
    return registrar(policy.deny(permission), "DENIED", permission);
  }

  public boolean denyPermission(Permission permission, Condition condition) {
    return registrar(policy.deny(permission, condition), "DENIED", permission);
  }

  /**
   * Remove a negação explícita. Não concede nada por si só — o usuário volta a
   * depender das permissões inline e das herdadas dos grupos.
   */
  public boolean allowPermission(Permission permission) {
    return registrar(policy.allow(permission), "deny removed", permission);
  }

  private boolean registrar(boolean mudou, String verbo, Permission permission) {
    if (mudou)
      System.out.println("[" + id + "] User permission " + verbo + ": " + permission);
    return mudou;
  }

  /** A política inline. Visível só para o núcleo, que é quem resolve acesso. */
  PermissionHolder getPolicy() {
    return policy;
  }

  /** As cláusulas inline, para inspeção — é o que permite imprimir a política. */
  @JsonIgnore
  public Set<Statement> getStatements() {
    return policy.getStatements();
  }

  @JsonIgnore
  public Set<Permission> getInlinePermissions() {
    return policy.getPermissions();
  }

  @JsonIgnore
  public Set<Permission> getDeniedPermissions() {
    return policy.getDeniedPermissions();
  }

  /** Esvazia a política inline (concessões e negações). */
  public void clearPermissions() {
    policy.clear();
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
  public ResourceType getType() {
    return PrincipalResource.USUARIO;
  }
}
