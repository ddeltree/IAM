package poo.iam;

import java.util.*;

import poo.iam.condition.Condition;
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

  public boolean grant(Permission permission, Condition condition) {
    return add(Statement.allow(permission, condition));
  }

  /** Ponte para condição ainda em código, durante a migração. */
  public boolean grant(Permission permission, PermissionCondition legado) {
    return add(Statement.allow(permission, legado));
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

  public boolean deny(Permission permission, Condition condition) {
    return add(Statement.deny(permission, condition));
  }

  public boolean deny(Permission permission, PermissionCondition legado) {
    return add(Statement.deny(permission, legado));
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
  public boolean permite(Permission permission, RequestContext ctx) {
    return concessaoQueAplica(permission, ctx) != null;
  }

  /** Existe negação aplicável a este pedido? */
  public boolean nega(Permission permission, RequestContext ctx) {
    return negacaoQueAplica(permission, ctx) != null;
  }

  /** A concessão que atende o pedido, ou {@code null}. Nomeá-la é o que
   *  permite explicar a decisão depois. */
  public Statement concessaoQueAplica(Permission permission, RequestContext ctx) {
    return primeira(Effect.ALLOW, permission, ctx);
  }

  /** A negação que barra o pedido, ou {@code null}. */
  public Statement negacaoQueAplica(Permission permission, RequestContext ctx) {
    return primeira(Effect.DENY, permission, ctx);
  }

  private Statement primeira(Effect efeito, Permission permission, RequestContext ctx) {
    for (Statement statement : statements) {
      if (statement.getEffect() == efeito && statement.aplica(permission, ctx))
        return statement;
    }
    return null;
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
