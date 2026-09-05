package poo.iam;

/**
 * Uma condição escrita como código, e não como dado.
 *
 * Toda condição desta aplicação já é dado — uma árvore de
 * {@link poo.iam.condition.Condition}, que se serializa, se explica e se
 * consulta. Esta interface ficou como saída de emergência: uma aplicação pode
 * ter uma regra que os operadores existentes não expressam, e é melhor ela
 * escrever a regra em Java do que o núcleo fingir uma tradução.
 *
 * O preço é declarado. Uma condição destas é opaca — veja
 * {@link poo.iam.condition.CondicaoOpaca} — e toda consulta responde "não sei"
 * sobre ela, nunca "verdadeiro". O motor continua avaliando normalmente; o que
 * se perde é a capacidade de perguntar quem pode e sobre o que se pode agir.
 */
@FunctionalInterface
public interface PermissionCondition {
  /** Concessão irrestrita: vale para qualquer recurso. */
  PermissionCondition SEMPRE = (principal, resource, context) -> true;

  boolean test(Principal principal, Resource resource, Object... context);

  /** Combina duas regras: basta uma passar. */
  default PermissionCondition or(PermissionCondition outra) {
    return (principal, resource, context) -> test(principal, resource, context)
        || outra.test(principal, resource, context);
  }

  /** Combina duas regras: as duas precisam passar. */
  default PermissionCondition and(PermissionCondition outra) {
    return (principal, resource, context) -> test(principal, resource, context)
        && outra.test(principal, resource, context);
  }
}
