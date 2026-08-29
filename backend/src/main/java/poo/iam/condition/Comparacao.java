package poo.iam.condition;

import java.util.List;
import java.util.Objects;
import java.util.Set;

import poo.iam.RequestContext;

/** Uma folha da árvore: operador, chave de contexto e os valores esperados. */
public final class Comparacao implements Condition {

  private final ConditionOperator operador;
  private final String chave;
  private final List<String> valores;

  public Comparacao(ConditionOperator operador, String chave, List<String> valores) {
    this.operador = operador;
    this.chave = chave;
    this.valores = List.copyOf(valores);
  }

  @Override
  public boolean avaliar(RequestContext ctx) {
    return operador.testar(ctx.get(chave), resolver(ctx));
  }

  /**
   * Expande as variáveis de política. {@code ${principal:id}} é o que permite
   * uma única cláusula servir todos os usuários — sem isso, "o autor pode
   * editar" não seria expressável como dado.
   */
  private List<String> resolver(RequestContext ctx) {
    return valores.stream().map(valor -> {
      if (!valor.startsWith("${") || !valor.endsWith("}"))
        return valor;
      var referencia = valor.substring(2, valor.length() - 1);
      var doContexto = ctx.get(referencia);
      // multivalorada não expande: seria ambíguo qual valor usar
      return doContexto.size() == 1 ? doContexto.get(0) : valor;
    }).toList();
  }

  @Override
  public Set<String> chaves() {
    return Set.of(chave);
  }

  @Override
  public <R> R accept(ConditionVisitor<R> visitor) {
    return visitor.visitarComparacao(this);
  }

  public ConditionOperator getOperador() {
    return operador;
  }

  public String getChave() {
    return chave;
  }

  public List<String> getValores() {
    return valores;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o)
      return true;
    if (!(o instanceof Comparacao))
      return false;
    var that = (Comparacao) o;
    return operador.name().equals(that.operador.name())
        && chave.equals(that.chave)
        && valores.equals(that.valores);
  }

  @Override
  public int hashCode() {
    return Objects.hash(operador.name(), chave, valores);
  }

  @Override
  public String toString() {
    return operador.name() + "(" + chave + ", " + valores + ")";
  }
}
