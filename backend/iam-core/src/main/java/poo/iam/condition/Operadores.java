package poo.iam.condition;

import java.util.List;

/**
 * Os operadores que o núcleo traz — o {@code StringEquals}, {@code Bool} e
 * afins da AWS.
 *
 * São constantes porque são valores: um operador é uma função de comparação,
 * imutável e sem estado. Quem resolve <em>nome</em> para operador, e quem
 * aceita operadores novos de uma aplicação, é o {@link OperatorRegistry} — que
 * é instância justamente para não virar mapa global disputado.
 */
public final class Operadores {

  /** Prefixo de conjunto da AWS: basta um valor do contexto casar. */
  public static final String PARA_ALGUM_VALOR = "ParaAlgumValor:";

  public static final ConditionOperator IGUAL = criar(
      "Igual", (contexto, politica) -> primeiro(contexto) != null
          && politica.contains(primeiro(contexto)));

  public static final ConditionOperator DIFERENTE = criar(
      "Diferente", (contexto, politica) -> !politica.contains(primeiro(contexto)));

  /** Comparação com curinga {@code *} e {@code ?}, como o StringLike. */
  public static final ConditionOperator PARECIDO = criar(
      "Parecido", (contexto, politica) -> {
        var valor = primeiro(contexto);
        if (valor == null)
          return false;
        return politica.stream().anyMatch(padrao -> casaComCuringa(valor, padrao));
      });

  public static final ConditionOperator BOOLEANO = criar(
      "Booleano", (contexto, politica) -> {
        var valor = primeiro(contexto);
        return valor != null && politica.contains(valor);
      });

  /**
   * A chave está ausente? É assim que se expressa uma ação sem alvo, como
   * criar uma turma: {@code Nulo { "recurso:id": "true" }}.
   */
  public static final ConditionOperator NULO = criar(
      "Nulo", (contexto, politica) -> {
        var ausente = contexto.isEmpty();
        return politica.contains(String.valueOf(ausente));
      });

  private Operadores() {
  }

  private static ConditionOperator criar(String nome, Comparacao comparacao) {
    return new ConditionOperator() {
      @Override
      public String name() {
        return nome;
      }

      @Override
      public boolean testar(List<String> doContexto, List<String> daPolitica) {
        return comparacao.testar(doContexto, daPolitica);
      }

      @Override
      public String toString() {
        return nome;
      }
    };
  }

  /** Aplica o operador valor a valor: basta um do contexto passar. */
  public static ConditionOperator paraAlgumValor(ConditionOperator base) {
    return new ConditionOperator() {
      @Override
      public String name() {
        return PARA_ALGUM_VALOR + base.name();
      }

      @Override
      public boolean testar(List<String> doContexto, List<String> daPolitica) {
        return doContexto.stream().anyMatch(v -> base.testar(List.of(v), daPolitica));
      }

      @Override
      public String toString() {
        return name();
      }
    };
  }

  private static String primeiro(List<String> valores) {
    return valores.isEmpty() ? null : valores.get(0);
  }

  private static boolean casaComCuringa(String valor, String padrao) {
    var regex = new StringBuilder("^");
    for (char c : padrao.toCharArray()) {
      switch (c) {
        case '*' -> regex.append(".*");
        case '?' -> regex.append('.');
        default -> regex.append(java.util.regex.Pattern.quote(String.valueOf(c)));
      }
    }
    return valor.matches(regex.append('$').toString());
  }

  @FunctionalInterface
  private interface Comparacao {
    boolean testar(List<String> doContexto, List<String> daPolitica);
  }
}
