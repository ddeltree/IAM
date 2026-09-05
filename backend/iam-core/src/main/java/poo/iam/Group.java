package poo.iam;

import java.util.*;

import poo.iam.condition.Condition;
import poo.iam.spi.PolicyListener;
import poo.iam.spi.PolicyListener.Mudanca;

public class Group implements Principal, Resource {
  private final String id;
  private final String name;
  private final Set<User> users = new HashSet<>();
  private final PermissionHolder policy = new PermissionHolder(); // composition
  private PolicyListener ouvinte = PolicyListener.SILENCIOSO;

  public Group(String name) {
    this(name, name);
  }

  /**
   * O nome é o identificador natural de um grupo — como na AWS, onde o nome é
   * único na conta e o ARN é montado a partir dele. Este construtor existe para
   * a aplicação que já tem um id próprio a informar.
   */
  public Group(String id, String name) {
    this.id = id;
    this.name = name;
  }

  @Override
  public String getId() {
    return id;
  }

  /** Grupos são gerenciados pelo IAM, então são recursos dele. */
  @Override
  public ResourceType getType() {
    return PrincipalResource.GRUPO;
  }

  protected void addUser(User user) {
    users.add(user);
  }

  protected void removeUser(User user) {
    users.remove(user);
  }

  /**
   * Os membros.
   *
   * Serializar os dois lados desta relação faria qualquer mapeador entrar em
   * recursão (usuário -> grupo -> usuário); evitar isso é responsabilidade de
   * quem serializa, não do núcleo.
   */
  public Set<User> getUsers() {
    return Collections.unmodifiableSet(users);
  }

  /** Remove todos os membros. */
  public void clearUsers() {
    users.clear();
  }

  public String getName() {
    return name;
  }

  /** Quem é avisado quando esta política muda. O padrão é ninguém. */
  public Group comOuvinte(PolicyListener ouvinte) {
    this.ouvinte = ouvinte == null ? PolicyListener.SILENCIOSO : ouvinte;
    return this;
  }

  // PERMISSIONS

  /** Concessão irrestrita para todos os membros. */
  public boolean grantPermission(Permission permission) {
    return registrar(policy.grant(permission), Mudanca.CONCEDIDA, permission);
  }

  /** Concessão válida só quando a condição passar. */
  public boolean grantPermission(Permission permission, Condition condition) {
    return registrar(policy.grant(permission, condition), Mudanca.CONCEDIDA, permission);
  }

  public boolean revokePermission(Permission permission) {
    return registrar(policy.revoke(permission), Mudanca.REVOGADA, permission);
  }

  /** Nega a permissão para todos os membros do grupo. */
  public boolean denyPermission(Permission permission) {
    return registrar(policy.deny(permission), Mudanca.NEGADA, permission);
  }

  public boolean denyPermission(Permission permission, Condition condition) {
    return registrar(policy.deny(permission, condition), Mudanca.NEGADA, permission);
  }

  /** Remove a negação explícita do grupo. Não concede a permissão. */
  public boolean allowPermission(Permission permission) {
    return registrar(policy.allow(permission), Mudanca.NEGACAO_REMOVIDA, permission);
  }

  private boolean registrar(boolean mudou, Mudanca mudanca, Permission permission) {
    if (mudou)
      ouvinte.politicaMudou(this, mudanca, permission);
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
  @Override
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
