package poo.iam.condition;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Converte a árvore de condições de e para a forma documental do bloco
 * {@code Condition} da AWS:
 *
 * <pre>
 * { "Igual": { "turma:professorId": ["${principal:id}"] } }
 * </pre>
 *
 * A forma são {@link Map}s, {@link List}s e textos — não uma árvore de alguma
 * biblioteca de JSON. É de propósito: assim o núcleo descreve o documento sem
 * escolher com o que a aplicação vai serializá-lo, e {@link #ler} aceita
 * exatamente o que {@link #escrever} produz. Quem transforma isso em texto é a
 * aplicação, com o mapeador que ela já usa.
 *
 * A escrita é um {@link ConditionVisitor}; a leitura despacha pelo registro de
 * {@link Operadores}, então acrescentar um operador não mexe aqui.
 */
public final class ConditionJson {

  private ConditionJson() {
  }

  /** {@code null} quando a condição é irrestrita — não há o que escrever. */
  public static Object escrever(Condition condicao) {
    return condicao.accept(new Escritor());
  }

  /** Lê o que {@link #escrever} produziu: {@code Map}, {@code List} e texto. */
  public static Condition ler(Object node) {
    if (node == null)
      return Condition.SEMPRE;
    if (!(node instanceof Map))
      throw new IllegalArgumentException(
          "Uma condição é um objeto de operadores, e veio " + node.getClass().getSimpleName());

    var partes = new ArrayList<Condition>();
    for (var entrada : ((Map<?, ?>) node).entrySet()) {
      var nome = String.valueOf(entrada.getKey());
      var valor = entrada.getValue();
      switch (nome) {
        case "TodasAs" -> partes.add(new TodasAs(lerLista(valor)));
        case "AlgumaDas" -> partes.add(new AlgumaDas(lerLista(valor)));
        case "Negacao" -> partes.add(new Negacao(ler(valor)));
        case "Opaca" -> throw new IllegalArgumentException(
            "Condição opaca não volta de documento: o que foi escrito é código, não dado");
        default -> partes.add(lerComparacoes(nome, valor));
      }
    }
    if (partes.isEmpty())
      return Condition.SEMPRE;
    // várias entradas no mesmo bloco são um E, como na AWS
    return partes.size() == 1 ? partes.get(0) : new TodasAs(partes);
  }

  private static List<Condition> lerLista(Object node) {
    if (!(node instanceof List))
      throw new IllegalArgumentException("TodasAs e AlgumaDas esperam uma lista de condições");
    var res = new ArrayList<Condition>();
    for (Object item : (List<?>) node)
      res.add(ler(item));
    return res;
  }

  /** {@code { "Igual": { "chave": [...], "outra": [...] } }} */
  private static Condition lerComparacoes(String operador, Object porChave) {
    var op = Operadores.get(operador);
    if (!(porChave instanceof Map))
      throw new IllegalArgumentException(
          "O operador " + operador + " espera um objeto de chave para valores");
    var res = new ArrayList<Condition>();
    for (var entrada : ((Map<?, ?>) porChave).entrySet())
      res.add(new Comparacao(op, String.valueOf(entrada.getKey()), textos(entrada.getValue())));
    return res.size() == 1 ? res.get(0) : new TodasAs(res);
  }

  private static List<String> textos(Object valor) {
    if (valor instanceof List<?> lista)
      return lista.stream().map(String::valueOf).toList();
    return List.of(String.valueOf(valor));
  }

  private static final class Escritor implements ConditionVisitor<Object> {

    @Override
    public Object visitarSempre(Sempre sempre) {
      return null;
    }

    @Override
    public Object visitarComparacao(Comparacao comparacao) {
      return Map.of(comparacao.getOperador().name(),
          Map.of(comparacao.getChave(), comparacao.getValores()));
    }

    @Override
    public Object visitarTodasAs(TodasAs todas) {
      return Map.of("TodasAs", escreverTodas(todas.getCondicoes()));
    }

    @Override
    public Object visitarAlgumaDas(AlgumaDas alguma) {
      return Map.of("AlgumaDas", escreverTodas(alguma.getCondicoes()));
    }

    @Override
    public Object visitarNegacao(Negacao negacao) {
      return Map.of("Negacao", negacao.getCondicao().accept(this));
    }

    @Override
    public Object visitarOpaca(CondicaoOpaca opaca) {
      // não dá para escrever código como documento; dizer isso é mais honesto
      // do que emitir algo que pareceria uma condição de verdade
      return Map.of("Opaca", "condição ainda em código");
    }

    private List<Object> escreverTodas(List<Condition> condicoes) {
      var res = new ArrayList<Object>();
      for (Condition c : condicoes) {
        var escrita = c.accept(this);
        res.add(escrita == null ? new LinkedHashMap<>() : escrita);
      }
      return res;
    }
  }
}
