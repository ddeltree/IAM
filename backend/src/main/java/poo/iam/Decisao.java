package poo.iam;

/**
 * O resultado de uma avaliação de acesso, com o motivo junto.
 *
 * Guardar a cláusula que decidiu é o que permite responder "por que foi
 * negado?" — o mesmo que a AWS devolve em {@code MatchedStatements} no policy
 * simulator. Sem isso, um 403 é indistinguível de outro.
 */
public final class Decisao {

  public enum Tipo {
    PERMITIDO,
    /** Uma cláusula DENY casou: nada pode reverter isso. */
    NEGACAO_EXPLICITA,
    /** Ninguém concedeu — o padrão do modelo é negar. */
    NEGACAO_PADRAO,
  }

  private final Tipo tipo;
  private final Statement decisiva;
  private final String origem;
  private final String motivo;

  private Decisao(Tipo tipo, Statement decisiva, String origem, String motivo) {
    this.tipo = tipo;
    this.decisiva = decisiva;
    this.origem = origem;
    this.motivo = motivo;
  }

  static Decisao permitido(Statement decisiva, String origem) {
    return new Decisao(Tipo.PERMITIDO, decisiva, origem,
        "concedido por " + decisiva.getSid() + " (" + origem + ")");
  }

  static Decisao negacaoExplicita(Statement decisiva, String origem) {
    return new Decisao(Tipo.NEGACAO_EXPLICITA, decisiva, origem,
        "negado explicitamente por " + decisiva.getSid() + " (" + origem + ")");
  }

  static Decisao negacaoPadrao(String motivo) {
    return new Decisao(Tipo.NEGACAO_PADRAO, null, null, motivo);
  }

  public boolean permitido() {
    return tipo == Tipo.PERMITIDO;
  }

  public Tipo getTipo() {
    return tipo;
  }

  /** A cláusula que decidiu, ou {@code null} quando foi a negação padrão. */
  public Statement getDecisiva() {
    return decisiva;
  }

  /** Onde estava a cláusula: {@code "inline"} ou o nome do grupo. */
  public String getOrigem() {
    return origem;
  }

  public String getMotivo() {
    return motivo;
  }

  @Override
  public String toString() {
    return tipo + ": " + motivo;
  }
}
