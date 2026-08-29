package poo.iam.condition;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.databind.JsonNode;

/**
 * Converte a árvore de condições de e para JSON, no formato do bloco
 * {@code Condition} da AWS:
 *
 * <pre>
 * { "Igual": { "turma:professorId": ["${principal:id}"] } }
 * </pre>
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

  public static Condition ler(JsonNode node) {
    if (node == null || node.isNull())
      return Condition.SEMPRE;

    var campos = node.fieldNames();
    var partes = new ArrayList<Condition>();
    while (campos.hasNext()) {
      var nome = campos.next();
      var valor = node.get(nome);
      switch (nome) {
        case "TodasAs" -> partes.add(new TodasAs(lerLista(valor)));
        case "AlgumaDas" -> partes.add(new AlgumaDas(lerLista(valor)));
        case "Negacao" -> partes.add(new Negacao(ler(valor)));
        default -> partes.add(lerComparacoes(nome, valor));
      }
    }
    if (partes.isEmpty())
      return Condition.SEMPRE;
    // várias entradas no mesmo bloco são um E, como na AWS
    return partes.size() == 1 ? partes.get(0) : new TodasAs(partes);
  }

  private static List<Condition> lerLista(JsonNode node) {
    var res = new ArrayList<Condition>();
    node.forEach(item -> res.add(ler(item)));
    return res;
  }

  /** {@code { "Igual": { "chave": [...], "outra": [...] } }} */
  private static Condition lerComparacoes(String operador, JsonNode porChave) {
    var op = Operadores.get(operador);
    var res = new ArrayList<Condition>();
    porChave.fieldNames().forEachRemaining(chave -> res.add(
        new Comparacao(op, chave, textos(porChave.get(chave)))));
    return res.size() == 1 ? res.get(0) : new TodasAs(res);
  }

  private static List<String> textos(JsonNode node) {
    if (node.isArray()) {
      var res = new ArrayList<String>();
      node.forEach(item -> res.add(item.asText()));
      return res;
    }
    return List.of(node.asText());
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
