package poo.iam.condition;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Registro dos operadores conhecidos.
 *
 * É registro, e não um {@code switch}, para que acrescentar um operador seja
 * uma linha e não uma edição no avaliador nem no desserializador.
 */
public final class Operadores {

  private static final Map<String, ConditionOperator> REGISTRO = new LinkedHashMap<>();

  /** Prefixo de conjunto da AWS: basta um valor do contexto casar. */
  public static final String PARA_ALGUM_VALOR = "ParaAlgumValor:";

  public static final ConditionOperator IGUAL = registrar(
      "Igual", (contexto, politica) -> primeiro(contexto) != null
          && politica.contains(primeiro(contexto)));

  public static final ConditionOperator DIFERENTE = registrar(
      "Diferente", (contexto, politica) -> !politica.contains(primeiro(contexto)));

  /** Comparação com curinga {@code *} e {@code ?}, como o StringLike. */
  public static final ConditionOperator PARECIDO = registrar(
      "Parecido", (contexto, politica) -> {
        var valor = primeiro(contexto);
        if (valor == null)
          return false;
        return politica.stream().anyMatch(padrao -> casaComCuringa(valor, padrao));
      });

  public static final ConditionOperator BOOLEANO = registrar(
      "Booleano", (contexto, politica) -> {
        var valor = primeiro(contexto);
        return valor != null && politica.contains(valor);
      });

  /**
   * A chave está ausente? É assim que se expressa uma ação sem alvo, como
   * criar uma turma: {@code Nulo { "recurso:id": "true" }}.
   */
  public static final ConditionOperator NULO = registrar(
      "Nulo", (contexto, politica) -> {
        var ausente = contexto.isEmpty();
        return politica.contains(String.valueOf(ausente));
      });

  private Operadores() {
  }

  private static ConditionOperator registrar(String nome, Comparacao comparacao) {
    var operador = new ConditionOperator() {
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
    REGISTRO.put(nome, operador);
    return operador;
  }

  public static void registrar(ConditionOperator operador) {
    REGISTRO.put(operador.name(), operador);
  }

  /**
   * Resolve o nome, entendendo o prefixo de conjunto. {@code ParaAlgumValor:}
   * é um decorador sobre um operador base, como na AWS — e não um operador
   * separado para cada combinação.
   */
  public static ConditionOperator get(String nome) {
    if (nome.startsWith(PARA_ALGUM_VALOR)) {
      var base = get(nome.substring(PARA_ALGUM_VALOR.length()));
      return paraAlgumValor(base);
    }
    var operador = REGISTRO.get(nome);
    if (operador == null)
      throw new IllegalArgumentException("Operador desconhecido: " + nome);
    return operador;
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
