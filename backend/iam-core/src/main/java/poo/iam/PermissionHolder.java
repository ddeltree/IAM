package poo.iam;

import java.util.*;
import java.util.stream.Collectors;

import poo.iam.condition.Condition;

/**
 * A política de um {@link User} ou de um {@link Group}: um conjunto de
 * {@link Statement}s.
 *
 * Guarda concessões e negações lado a lado, sem uma apagar a outra, e não
 * decide nada — casar cláusula com pedido é do {@link AccessResolver}, que
 * precisa fazer isso percorrendo vários principais de uma vez. Aqui só se
 * guarda e se lê, o que é justamente o que {@link Principal} pede de quem
 * implementa a interface por fora.
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
