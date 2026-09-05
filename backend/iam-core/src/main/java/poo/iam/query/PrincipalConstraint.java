package poo.iam.query;

import java.util.LinkedHashSet;
import java.util.Set;

/**
 * Quem uma cláusula pode alcançar, depois de fixado o recurso.
 *
 * É sempre um <em>superconjunto</em> do conjunto verdadeiro: serve para
 * escolher candidatos, nunca para decidir. Quando a restrição não é
 * determinável, ela vira {@link #todos()} — e a consulta cai na varredura, que
 * custa desempenho e não corretude.
 */
public final class PrincipalConstraint {

  private static final PrincipalConstraint TODOS = new PrincipalConstraint(null);

  /** {@code null} significa "não sei restringir". */
  private final Set<String> ids;

  private PrincipalConstraint(Set<String> ids) {
    this.ids = ids;
  }

  public static PrincipalConstraint todos() {
    return TODOS;
  }

  public static PrincipalConstraint ninguem() {
    return new PrincipalConstraint(Set.of());
  }

  public static PrincipalConstraint apenas(Set<String> ids) {
    return new PrincipalConstraint(Set.copyOf(ids));
  }

  public boolean irrestrita() {
    return ids == null;
  }

  public Set<String> getIds() {
    return ids;
  }

  /** E lógico: o desconhecido não amplia, então o lado conhecido prevalece. */
  public PrincipalConstraint e(PrincipalConstraint outra) {
    if (irrestrita())
      return outra;
    if (outra.irrestrita())
      return this;
    var res = new LinkedHashSet<>(ids);
    res.retainAll(outra.ids);
    return apenas(res);
  }

  /** OU lógico: basta um lado ser desconhecido para o todo ser desconhecido. */
  public PrincipalConstraint ou(PrincipalConstraint outra) {
    if (irrestrita() || outra.irrestrita())
      return todos();
    var res = new LinkedHashSet<>(ids);
    res.addAll(outra.ids);
    return apenas(res);
  }

  @Override
  public String toString() {
    return irrestrita() ? "qualquer principal" : "principais " + ids;
  }
}
