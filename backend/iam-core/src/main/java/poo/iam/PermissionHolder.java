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
  private final List<Policy> anexadas = new ArrayList<>();

  /**
   * Anexa uma política nomeada. As cláusulas dela passam a valer sem serem
   * copiadas para cá — mudá-la muda o acesso de todos que a têm anexada.
   */
  public boolean anexar(Policy policy) {
    return anexadas.contains(policy) ? false : anexadas.add(policy);
  }

  public boolean desanexar(Policy policy) {
    return anexadas.remove(policy);
  }

  public List<Policy> getPoliticasAnexadas() {
    return Collections.unmodifiableList(anexadas);
  }

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

  /**
   * Remove exatamente esta cláusula.
   *
   * {@link #revoke(Permission)} não serve para tudo: ele apaga <em>todas</em> as
   * concessões de uma permissão e, por receber uma {@code Permission}, nem
   * alcança uma cláusula escrita com curinga. Quem edita política uma linha por
   * vez precisa disto.
   */
  public boolean remover(Statement statement) {
    return statements.remove(statement);
  }

  /** Remove a cláusula com este {@code sid}, seja qual for o efeito. */
  public boolean removerPorSid(String sid) {
    return statements.removeIf(s -> s.getSid().equals(sid));
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

  /**
   * Todas as cláusulas que valem: as inline primeiro, depois as das políticas
   * anexadas, na ordem em que foram anexadas.
   *
   * A ordem não muda a decisão — negação explícita vence esteja onde estiver —,
   * mas muda qual cláusula é <em>nomeada</em> como decisiva, e uma explicação
   * que muda de resposta entre execuções não explica nada.
   */
  public Set<Statement> getStatements() {
    if (anexadas.isEmpty())
      return Collections.unmodifiableSet(statements);

    var todas = new LinkedHashSet<>(statements);
    for (Policy policy : anexadas)
      todas.addAll(policy.getStatements());
    return Collections.unmodifiableSet(todas);
  }

  /** Só as inline, sem as das políticas anexadas. */
  public Set<Statement> getStatementsInline() {
    return Collections.unmodifiableSet(statements);
  }

  public Set<Permission> getPermissions() {
    return permissoesCom(Effect.ALLOW);
  }

  public Set<Permission> getDeniedPermissions() {
    return permissoesCom(Effect.DENY);
  }

  /**
   * As permissões nomeadas exatamente por alguma cláusula deste efeito.
   *
   * Uma cláusula com curinga fica de fora: ela não corresponde a uma permissão,
   * e sim a um conjunto que só se enumera sabendo o que existe. Quem responde
   * "o que este principal pode?" contando com curingas é
   * {@link EffectivePermissions}, que tem o catálogo.
   */
  private Set<Permission> permissoesCom(Effect efeito) {
    return statements.stream()
        .filter(s -> s.getEffect() == efeito)
        .map(Statement::getPermission)
        .filter(Objects::nonNull)
        .collect(Collectors.toUnmodifiableSet());
  }

  /** Esvazia a política. */
  public void clear() {
    statements.clear();
    anexadas.clear();
  }
}
