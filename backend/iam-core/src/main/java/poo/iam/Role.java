package poo.iam;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * Um conjunto de permissões que se assume temporariamente — o <em>role</em> da
 * AWS.
 *
 * Um papel não é um grupo. A política de um grupo vale o tempo todo para quem
 * está nele; a de um papel só vale enquanto alguém o está exercendo, e essa
 * pessoa continua sendo ela mesma fora dali. É o que separa "o professor é
 * moderador" de "o professor pode passar a moderar quando precisar" — e a
 * diferença aparece no registro: uma exclusão feita sob o papel de moderador
 * se distingue de uma feita como professor.
 *
 * <h2>Duas políticas, não uma</h2>
 *
 * A {@link #getStatements() de permissão} diz o que o papel pode. A
 * {@link #getConfianca() de confiança} diz quem pode assumi-lo, e é uma
 * política <em>de recurso</em>: o papel é o recurso, e assumir é a ação. Não
 * foi preciso inventar mecanismo para isso — a política no recurso já existia,
 * e a AWS faz exatamente o mesmo.
 */
public final class Role implements Principal, Resource {

  private final String id;
  private final String nome;
  private final PermissionHolder politica = new PermissionHolder();
  private final Set<Statement> confianca = new LinkedHashSet<>();

  public Role(String nome) {
    this(nome, nome);
  }

  public Role(String id, String nome) {
    this.id = id;
    this.nome = nome;
  }

  @Override
  public String getId() {
    return id;
  }

  @Override
  public String getName() {
    return nome;
  }

  @Override
  public ResourceType getType() {
    return PrincipalResource.PAPEL;
  }

  // ---------- o que o papel pode ----------

  public boolean add(Statement statement) {
    return politica.add(statement);
  }

  public boolean anexar(Policy policy) {
    return politica.anexar(policy);
  }

  @Override
  public Set<Statement> getStatements() {
    return politica.getStatements();
  }

  // ---------- quem pode assumi-lo ----------

  /**
   * Autoriza alguém a assumir este papel.
   *
   * A condição diz quem: {@code Igual {"principal:id": "7"}} para uma pessoa,
   * {@code ParaAlgumValor:Igual {"principal:groups": "Professores"}} para um
   * grupo inteiro.
   */
  public Role confiaEm(poo.iam.condition.Condition quem) {
    confianca.add(Statement.de(Effect.ALLOW,
        ActionPattern.de(IamAction.ASSUMIR_PAPEL),
        ResourcePattern.de(this),
        quem, "confianca:" + nome));
    return this;
  }

  /** A política de confiança, na forma em que o motor a avalia. */
  public Policy getConfianca() {
    return new Policy("confianca:" + nome, Collections.unmodifiableSet(confianca));
  }

  @Override
  public String toString() {
    return "papel " + nome;
  }
}
