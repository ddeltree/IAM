package poo.iam;

import java.util.*;
import java.util.stream.Collectors;

/**
 * A política de um {@link User} ou de um {@link Group}: um conjunto de
 * {@link Statement}s.
 *
 * Guarda concessões e negações lado a lado, sem uma apagar a outra — quem
 * resolve o conflito é o {@link AccessResolver}, aplicando "deny overrides".
 */
public class PermissionHolder {
  private final Set<Statement> statements = new LinkedHashSet<>();

  public boolean add(Statement statement) {
    return statements.add(statement);
  }

  public boolean grant(Permission permission) {
    return add(Statement.allow(permission));
  }

  public boolean grant(Permission permission, PermissionCondition condition) {
    return add(Statement.allow(permission, condition));
  }

  /** Remove todas as concessões desta permissão, com ou sem condição. */
  public boolean revoke(Permission permission) {
    return statements.removeIf(
        s -> s.getEffect() == Effect.ALLOW && s.falaSobre(permission));
  }

  /** Nega a permissão explicitamente, sobrepondo qualquer concessão. */
  public boolean deny(Permission permission) {
    return add(Statement.deny(permission));
  }

  public boolean deny(Permission permission, PermissionCondition condition) {
    return add(Statement.deny(permission, condition));
  }

  /**
   * Remove a negação explícita. Não concede a permissão: se ela não estiver
   * concedida aqui nem em um grupo, o acesso continua barrado.
   */
  public boolean allow(Permission permission) {
    return statements.removeIf(
        s -> s.getEffect() == Effect.DENY && s.falaSobre(permission));
  }

  /** Existe concessão aplicável a este pedido? */
  public boolean permite(Permission permission, User user, Resource resource, Object... context) {
    return algum(Effect.ALLOW, permission, user, resource, context);
  }

  /** Existe negação aplicável a este pedido? */
  public boolean nega(Permission permission, User user, Resource resource, Object... context) {
    return algum(Effect.DENY, permission, user, resource, context);
  }

  private boolean algum(Effect efeito, Permission permission, User user, Resource resource, Object... context) {
    for (Statement statement : statements) {
      if (statement.getEffect() == efeito && statement.aplica(permission, user, resource, context))
        return true;
    }
    return false;
  }

  /** Ignora a condição: diz apenas que existe uma cláusula sobre a permissão. */
  public boolean has(Permission permission) {
    return statements.stream()
        .anyMatch(s -> s.getEffect() == Effect.ALLOW && s.falaSobre(permission));
  }

  public boolean isDenied(Permission permission) {
    return statements.stream()
        .anyMatch(s -> s.getEffect() == Effect.DENY && s.falaSobre(permission));
  }

  public Set<Statement> getStatements() {
    return Collections.unmodifiableSet(statements);
  }

  public Set<Permission> getPermissions() {
    return permissoesCom(Effect.ALLOW);
  }

  public Set<Permission> getDeniedPermissions() {
    return permissoesCom(Effect.DENY);
  }

  private Set<Permission> permissoesCom(Effect efeito) {
    return statements.stream()
        .filter(s -> s.getEffect() == efeito)
        .map(Statement::getPermission)
        .collect(Collectors.toUnmodifiableSet());
  }

  /** Esvazia a política. */
  public void clear() {
    statements.clear();
  }
}
