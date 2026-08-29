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
