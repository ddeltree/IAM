package poo.iam.query;

import java.util.List;

/**
 * O que sobra de uma condição quando se fixa o principal e se deixa o recurso
 * em aberto: um filtro sobre atributos.
 *
 * É o mesmo objeto que vira um {@code Predicate} em memória hoje e uma cláusula
 * {@code WHERE} amanhã — a ponte entre o motor e o banco. Como a
 * {@link PrincipalConstraint}, descreve um superconjunto: serve para escolher
 * candidatos, nunca para decidir.
 */
public interface ResourceConstraint {

  <R> R accept(ConstraintVisitor<R> visitor);

  final class Tudo implements ResourceConstraint {
    public static final Tudo INSTANCIA = new Tudo();

    private Tudo() {
    }

    @Override
    public <R> R accept(ConstraintVisitor<R> v) {
      return v.visitarTudo(this);
    }

    @Override
    public String toString() {
      return "tudo";
    }
  }

  final class Nada implements ResourceConstraint {
    public static final Nada INSTANCIA = new Nada();

    private Nada() {
    }

    @Override
    public <R> R accept(ConstraintVisitor<R> v) {
      return v.visitarNada(this);
    }

    @Override
    public String toString() {
      return "nada";
    }
  }

  /** O atributo do recurso é igual a este valor. */
  final class AtributoIgual implements ResourceConstraint {
    private final String chave;
    private final String valor;

    public AtributoIgual(String chave, String valor) {
      this.chave = chave;
      this.valor = valor;
    }

    public String getChave() {
      return chave;
    }

    public String getValor() {
      return valor;
    }

    @Override
    public <R> R accept(ConstraintVisitor<R> v) {
      return v.visitarIgual(this);
    }

    @Override
    public String toString() {
      return chave + " = " + valor;
    }
  }

  /** O atributo multivalorado do recurso contém este valor. */
  final class AtributoContem implements ResourceConstraint {
    private final String chave;
    private final String valor;

    public AtributoContem(String chave, String valor) {
      this.chave = chave;
      this.valor = valor;
    }

    public String getChave() {
      return chave;
    }

    public String getValor() {
      return valor;
    }

    @Override
    public <R> R accept(ConstraintVisitor<R> v) {
      return v.visitarContem(this);
    }

    @Override
    public String toString() {
      return chave + " contém " + valor;
    }
  }

  /**
   * O atributo comparado por ordem — {@code >}, {@code >=}, {@code <},
   * {@code <=} — para as condições numéricas e de tempo.
   *
   * Sem este nó, uma condição como "a atividade ainda não venceu" cairia em
   * {@link Tudo} na extração: correto, porque um superconjunto nunca esconde
   * nada, mas inútil — o filtro não filtraria e o banco varreria a tabela
   * inteira para o motor descartar depois. Operador novo sem nó de restrição
   * correspondente é expressividade comprada com consultabilidade.
   */
  final class AtributoCompara implements ResourceConstraint {
    /** O símbolo, já em forma comparável: {@code >}, {@code >=}, {@code <}, {@code <=}. */
    private final String operador;
    private final String chave;
    private final String valor;

    public AtributoCompara(String chave, String operador, String valor) {
      this.chave = chave;
      this.operador = operador;
      this.valor = valor;
    }

    public String getChave() {
      return chave;
    }

    public String getOperador() {
      return operador;
    }

    public String getValor() {
      return valor;
    }

    @Override
    public <R> R accept(ConstraintVisitor<R> v) {
      return v.visitarCompara(this);
    }

    @Override
    public String toString() {
      return chave + " " + operador + " " + valor;
    }
  }

  /**
   * O recurso está nesta lista de ids.
   *
   * Vem das políticas anexadas aos próprios recursos, e é diferente em espécie
   * dos outros nós: eles são regras sobre atributos, e este é uma enumeração.
   * Não dá para derivá-lo de uma condição — foi preciso perguntar ao provedor
   * quais recursos têm política própria. Sem ele, um recurso compartilhado
   * sumiria da resposta de "sobre o que posso agir", que é o modo mais
   * silencioso de perder consultabilidade.
   */
  final class IdEm implements ResourceConstraint {
    private final java.util.Set<String> ids;

    public IdEm(java.util.Collection<String> ids) {
      this.ids = java.util.Set.copyOf(ids);
    }

    public java.util.Set<String> getIds() {
      return ids;
    }

    @Override
    public <R> R accept(ConstraintVisitor<R> v) {
      return v.visitarIdEm(this);
    }

    @Override
    public String toString() {
      return "id em " + ids;
    }
  }

  final class Todas implements ResourceConstraint {
    private final List<ResourceConstraint> partes;

    public Todas(List<ResourceConstraint> partes) {
      this.partes = List.copyOf(partes);
    }

    public List<ResourceConstraint> getPartes() {
      return partes;
    }

    @Override
    public <R> R accept(ConstraintVisitor<R> v) {
      return v.visitarTodas(this);
    }

    @Override
    public String toString() {
      return "todas" + partes;
    }
  }

  final class Alguma implements ResourceConstraint {
    private final List<ResourceConstraint> partes;

    public Alguma(List<ResourceConstraint> partes) {
      this.partes = List.copyOf(partes);
    }

    public List<ResourceConstraint> getPartes() {
      return partes;
    }

    @Override
    public <R> R accept(ConstraintVisitor<R> v) {
      return v.visitarAlguma(this);
    }

    @Override
    public String toString() {
      return "alguma" + partes;
    }
  }
}
