package poo.iam.condition;

/**
 * Percorre a árvore de condições. É o que permite fazer outras coisas com uma
 * política além de executá-la: imprimir, explicar, avaliar parcialmente,
 * traduzir para uma cláusula {@code WHERE}.
 */
public interface ConditionVisitor<R> {

  R visitarSempre(Sempre sempre);

  R visitarComparacao(Comparacao comparacao);

  R visitarTodasAs(TodasAs todas);

  R visitarAlgumaDas(AlgumaDas alguma);

  R visitarNegacao(Negacao negacao);

  /**
   * Uma condição que ainda é código. Um visitante honesto responde "não sei" —
   * nunca "verdadeiro".
   */
  R visitarOpaca(CondicaoOpaca opaca);
}
