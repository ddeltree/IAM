package poo.iam;

/**
 * Regra contextual de uma concessão — o bloco {@code Condition} da AWS.
 *
 * Recebe quem está pedindo, sobre qual recurso, e o contexto da requisição
 * (equivalente às chaves {@code aws:*}: hora, origem, o que a aplicação quiser
 * passar adiante).
 */
@FunctionalInterface
public interface PermissionCondition {
  /** Concessão irrestrita: vale para qualquer recurso. */
  PermissionCondition SEMPRE = (user, resource, context) -> true;

  boolean test(User user, Resource resource, Object... context);

  /** Combina duas regras: basta uma passar. */
  default PermissionCondition or(PermissionCondition outra) {
    return (user, resource, context) -> test(user, resource, context)
        || outra.test(user, resource, context);
  }

  /** Combina duas regras: as duas precisam passar. */
  default PermissionCondition and(PermissionCondition outra) {
    return (user, resource, context) -> test(user, resource, context)
        && outra.test(user, resource, context);
  }
}
