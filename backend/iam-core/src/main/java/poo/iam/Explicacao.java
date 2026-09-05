package poo.iam;

import java.util.Collections;
import java.util.List;

/**
 * Tudo o que o motor considerou para chegar a uma decisão.
 *
 * {@link Decisao} responde "pode?" e nomeia a cláusula que decidiu. Isto
 * responde "por quê?" — e, principalmente, <em>por que não</em>: quando nada
 * concede, a decisão não tem o que nomear, e "nenhuma cláusula concede" não
 * explica por que as cláusulas que existem não serviram.
 *
 * É o {@code MatchedStatements} que o policy simulator da AWS devolve, mais o
 * contexto: sem ver as chaves resolvidas, "a condição não passou" continua sem
 * dizer qual valor foi comparado com qual.
 */
public final class Explicacao {

  /** Uma cláusula que fala sobre a permissão pedida, e o que houve com ela. */
  public static final class ClausulaAvaliada {

    private final Statement statement;
    private final String origem;
    private final boolean alcancaORecurso;
    private final boolean condicaoPassou;
    private final boolean decisiva;

    ClausulaAvaliada(Statement statement, String origem, boolean alcancaORecurso,
        boolean condicaoPassou, boolean decisiva) {
      this.statement = statement;
      this.origem = origem;
      this.alcancaORecurso = alcancaORecurso;
      this.condicaoPassou = condicaoPassou;
      this.decisiva = decisiva;
    }

    public Statement getStatement() {
      return statement;
    }

    /** {@code "inline"}, o nome do principal de quem veio, ou {@code TIPO/id}. */
    public String getOrigem() {
      return origem;
    }

    /**
     * O padrão de recurso da cláusula alcança o recurso perguntado?
     *
     * Separado da condição de propósito: "escrevi a cláusula e ela não vale"
     * tem duas causas bem diferentes, e confundi-las faz procurar erro na
     * condição quando o problema é a cláusula mirar outro recurso.
     */
    public boolean alcancaORecurso() {
      return alcancaORecurso;
    }

    public boolean condicaoPassou() {
      return condicaoPassou;
    }

    /** Foi esta que decidiu o pedido? */
    public boolean isDecisiva() {
      return decisiva;
    }

    /** Esta cláusula, sozinha, atenderia o pedido? */
    public boolean aplicaria() {
      return alcancaORecurso && condicaoPassou;
    }

    @Override
    public String toString() {
      var estado = !alcancaORecurso ? "outro recurso"
          : condicaoPassou ? "aplica" : "condição não passou";
      return statement.getSid() + " (" + origem + ") — " + estado;
    }
  }

  private final Decisao decisao;
  private final RequestContext contexto;
  private final List<ClausulaAvaliada> clausulas;
  private final int clausulasAlcancadas;

  Explicacao(Decisao decisao, RequestContext contexto, List<ClausulaAvaliada> clausulas,
      int clausulasAlcancadas) {
    this.decisao = decisao;
    this.contexto = contexto;
    this.clausulas = Collections.unmodifiableList(clausulas);
    this.clausulasAlcancadas = clausulasAlcancadas;
  }

  /** A decisão do motor. Não é recalculada aqui — é a mesma, pedida a ele. */
  public Decisao getDecisao() {
    return decisao;
  }

  /**
   * As chaves sobre as quais as condições decidiram.
   *
   * É o que transforma "a condição não passou" em "a condição comparou
   * {@code turma:professorId=[7]} com {@code principal:id=[9]}".
   */
  public RequestContext getContexto() {
    return contexto;
  }

  /** As cláusulas que falam sobre a permissão pedida, na ordem da decisão. */
  public List<ClausulaAvaliada> getClausulas() {
    return clausulas;
  }

  /**
   * Quantas cláusulas o motor percorreu ao todo, incluindo as que falam de
   * outras ações. A diferença para {@code getClausulas().size()} é o que diz
   * "você tem 30 cláusulas e nenhuma menciona esta ação".
   */
  public int getClausulasAlcancadas() {
    return clausulasAlcancadas;
  }

  @Override
  public String toString() {
    return decisao + " — " + clausulas.size() + " de " + clausulasAlcancadas
        + " cláusulas falam sobre isso";
  }
}
