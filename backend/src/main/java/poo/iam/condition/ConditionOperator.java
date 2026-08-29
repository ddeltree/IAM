package poo.iam.condition;

import java.util.List;

/**
 * Como dois conjuntos de valores são comparados — o {@code StringEquals},
 * {@code Bool} e afins da AWS.
 *
 * Recebe os valores que vieram do contexto e os que estão escritos na política,
 * ambos já como texto: é isso que mantém a condição serializável.
 */
public interface ConditionOperator {

  String name();

  boolean testar(List<String> doContexto, List<String> daPolitica);
}
