package poo.iam.condition;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Resolve o nome de um operador escrito numa política.
 *
 * Um documento diz {@code {"Igual": {...}}}; alguém precisa transformar
 * {@code "Igual"} no operador que compara. É registro, e não um {@code switch},
 * para que uma aplicação possa acrescentar um operador que o núcleo não previu
 * — e é instância, e não estático, para que duas aplicações no mesmo processo
 * não disputem o mesmo mapa.
 *
 * O prefixo de conjunto {@code ParaAlgumValor:} é entendido como decorador
 * sobre um operador base, como na AWS faz com {@code ForAnyValue:} — e não como
 * um operador separado para cada combinação.
 */
public final class OperatorRegistry {

  /** Só os embutidos, imutável: serve de semente e não aceita registro. */
  private static final Map<String, ConditionOperator> EMBUTIDOS = Map.ofEntries(
      Map.entry(Operadores.IGUAL.name(), Operadores.IGUAL),
      Map.entry(Operadores.DIFERENTE.name(), Operadores.DIFERENTE),
      Map.entry(Operadores.PARECIDO.name(), Operadores.PARECIDO),
      Map.entry(Operadores.BOOLEANO.name(), Operadores.BOOLEANO),
      Map.entry(Operadores.NULO.name(), Operadores.NULO),
      Map.entry(Operadores.MAIOR.name(), Operadores.MAIOR),
      Map.entry(Operadores.MAIOR_OU_IGUAL.name(), Operadores.MAIOR_OU_IGUAL),
      Map.entry(Operadores.MENOR.name(), Operadores.MENOR),
      Map.entry(Operadores.MENOR_OU_IGUAL.name(), Operadores.MENOR_OU_IGUAL),
      Map.entry(Operadores.DATA_DEPOIS.name(), Operadores.DATA_DEPOIS),
      Map.entry(Operadores.DATA_ANTES.name(), Operadores.DATA_ANTES));

  private static final OperatorRegistry PADRAO = new OperatorRegistry();

  private final Map<String, ConditionOperator> registro = new LinkedHashMap<>(EMBUTIDOS);

  /** Um registro novo, já com os operadores embutidos. */
  public OperatorRegistry() {
  }

  /**
   * O registro só com os embutidos, compartilhado.
   *
   * É seguro compartilhar porque ninguém o modifica: quem acrescenta operador
   * constrói o seu com {@code new OperatorRegistry()}. Existe para o caso
   * comum, em que a política usa só o vocabulário que o núcleo já traz.
   */
  public static OperatorRegistry padrao() {
    return PADRAO;
  }

  public OperatorRegistry registrar(ConditionOperator operador) {
    if (this == PADRAO)
      throw new IllegalStateException(
          "O registro padrão é compartilhado e não aceita operadores novos; "
              + "construa um com new OperatorRegistry()");
    registro.put(operador.name(), operador);
    return this;
  }

  /**
   * Resolve o nome, entendendo os prefixos como decoradores sobre um operador
   * base — que é como a AWS os trata, e a razão de não haver um operador
   * separado para cada combinação. Eles se compõem:
   * {@code SeExistir:ParaAlgumValor:Igual} é um nome válido.
   */
  public ConditionOperator get(String nome) {
    if (nome.startsWith(Operadores.PARA_ALGUM_VALOR))
      return Operadores.paraAlgumValor(get(nome.substring(Operadores.PARA_ALGUM_VALOR.length())));
    if (nome.startsWith(Operadores.PARA_TODO_VALOR))
      return Operadores.paraTodoValor(get(nome.substring(Operadores.PARA_TODO_VALOR.length())));
    if (nome.startsWith(Operadores.SE_EXISTIR))
      return Operadores.seExistir(get(nome.substring(Operadores.SE_EXISTIR.length())));

    var operador = registro.get(nome);
    if (operador == null)
      throw new IllegalArgumentException("Operador desconhecido: " + nome);
    return operador;
  }

  public boolean conhece(String nome) {
    return registro.containsKey(nome);
  }
}
