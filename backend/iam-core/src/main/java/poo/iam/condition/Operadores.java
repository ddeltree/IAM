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
        return politica.stream().anyMatch(padrao -> poo.iam.Curinga.casa(valor, padrao));
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

  // ---------- numéricos ----------

  public static final ConditionOperator MAIOR = numerico("Maior", (a, b) -> a > b);
  public static final ConditionOperator MAIOR_OU_IGUAL = numerico("MaiorOuIgual", (a, b) -> a >= b);
  public static final ConditionOperator MENOR = numerico("Menor", (a, b) -> a < b);
  public static final ConditionOperator MENOR_OU_IGUAL = numerico("MenorOuIgual", (a, b) -> a <= b);

  // ---------- de tempo ----------

  /**
   * Compara instantes, datas ou horas — o {@code DateGreaterThan} da AWS.
   *
   * Aceita as três formas ISO-8601 ({@code 2026-09-04T22:30:00Z},
   * {@code 2026-09-04}, {@code 22:30}) porque as três aparecem: a chave
   * {@code contexto:hora} serve a "só durante a aula" e {@code contexto:data} a
   * "só até a data de entrega". Comparar formas diferentes entre si não é
   * pergunta que faça sentido, e devolve falso em vez de um palpite.
   */
  public static final ConditionOperator DATA_DEPOIS = temporal("DataDepois", cmp -> cmp > 0);
  public static final ConditionOperator DATA_ANTES = temporal("DataAntes", cmp -> cmp < 0);

  // ---------- prefixos ----------

  /** Prefixo da AWS: se a chave não existe no contexto, a condição passa. */
  public static final String SE_EXISTIR = "SeExistir:";

  /** Prefixo de conjunto da AWS: todos os valores do contexto precisam casar. */
  public static final String PARA_TODO_VALOR = "ParaTodoValor:";

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

  /**
   * Aplica o operador a todos os valores do contexto — o {@code ForAllValues:}
   * da AWS, com a mesma armadilha que ela documenta: <b>chave ausente passa</b>,
   * porque "todos os zero valores casam" é verdade. Numa cláusula ALLOW isso
   * concede quando não se pretendia; use com NULO ao lado, ou num DENY.
   */
  public static ConditionOperator paraTodoValor(ConditionOperator base) {
    return new ConditionOperator() {
      @Override
      public String name() {
        return PARA_TODO_VALOR + base.name();
      }

      @Override
      public boolean testar(List<String> doContexto, List<String> daPolitica) {
        return doContexto.stream().allMatch(v -> base.testar(List.of(v), daPolitica));
      }

      @Override
      public String toString() {
        return name();
      }
    };
  }

  /**
   * Chave ausente passa; presente, decide o operador base — o sufixo
   * {@code IfExists} da AWS. Serve para "se a requisição informou a hora, ela
   * precisa estar na janela", sem exigir que sempre informe.
   */
  public static ConditionOperator seExistir(ConditionOperator base) {
    return new ConditionOperator() {
      @Override
      public String name() {
        return SE_EXISTIR + base.name();
      }

      @Override
      public boolean testar(List<String> doContexto, List<String> daPolitica) {
        return doContexto.isEmpty() || base.testar(doContexto, daPolitica);
      }

      @Override
      public String toString() {
        return name();
      }
    };
  }

  private static ConditionOperator numerico(String nome, java.util.function.BiPredicate<Double, Double> teste) {
    return criar(nome, (contexto, politica) -> {
      var valor = numero(primeiro(contexto));
      if (valor == null)
        return false;
      // basta um valor da política satisfazer, como nos demais operadores
      return politica.stream().map(Operadores::numero)
          .filter(java.util.Objects::nonNull)
          .anyMatch(limite -> teste.test(valor, limite));
    });
  }

  private static Double numero(String texto) {
    if (texto == null)
      return null;
    try {
      return Double.valueOf(texto);
    } catch (NumberFormatException e) {
      return null;
    }
  }

  private static ConditionOperator temporal(String nome, java.util.function.IntPredicate teste) {
    return criar(nome, (contexto, politica) -> {
      var valor = primeiro(contexto);
      if (valor == null)
        return false;
      return politica.stream().anyMatch(limite -> {
        var cmp = comparar(valor, limite);
        return cmp != null && teste.test(cmp);
      });
    });
  }

  /**
   * Compara dois textos ISO-8601 da mesma forma, ou {@code null} se não forem
   * comparáveis. Não há palpite aqui: uma data contra uma hora devolve nulo, e
   * o operador responde falso.
   */
  private static Integer comparar(String a, String b) {
    try {
      if (a.contains("T"))
        return java.time.OffsetDateTime.parse(a).compareTo(java.time.OffsetDateTime.parse(b));
      if (a.contains("-"))
        return java.time.LocalDate.parse(a).compareTo(java.time.LocalDate.parse(b));
      if (a.contains(":"))
        return java.time.LocalTime.parse(a).compareTo(java.time.LocalTime.parse(b));
    } catch (RuntimeException naoEhDoMesmoTipo) {
      return null;
    }
    return null;
  }

  private static String primeiro(List<String> valores) {
    return valores.isEmpty() ? null : valores.get(0);
  }

  @FunctionalInterface
  private interface Comparacao {
    boolean testar(List<String> doContexto, List<String> daPolitica);
  }
}
