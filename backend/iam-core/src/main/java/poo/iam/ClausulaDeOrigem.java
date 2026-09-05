package poo.iam;

/**
 * Uma cláusula e de onde ela vem.
 *
 * {@link Principal#getStatements()} devolve só as cláusulas próprias — as dos
 * grupos, dos papéis e das políticas anexadas o motor alcança percorrendo o
 * grafo. Quem quiser <em>mostrar</em> a política de alguém precisa da soma, e
 * precisa dela rotulada: uma lista achatada esconde exatamente o que a pergunta
 * "por que ele pode isso?" quer saber.
 */
public record ClausulaDeOrigem(Statement statement, String origem) {

  /** {@code "inline"} quando é do próprio principal. */
  public boolean isInline() {
    return "inline".equals(origem);
  }

  @Override
  public String toString() {
    return statement.getSid() + " (" + origem + ")";
  }
}
