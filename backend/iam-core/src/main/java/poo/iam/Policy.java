package poo.iam;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.Set;

/**
 * Um conjunto de cláusulas com nome, anexável a vários principais — a
 * <em>managed policy</em> da AWS.
 *
 * A diferença para a política inline não é técnica, é de propriedade. Uma
 * cláusula inline pertence a um principal e morre com ele; uma política nomeada
 * existe por si, e mudá-la muda o acesso de todos que a têm anexada de uma vez.
 * Sem isso, "os professores e os monitores podem o mesmo" se escreve copiando
 * as cláusulas nos dois lugares — e a cópia envelhece.
 *
 * É imutável: anexar a mesma política a dez principais não abre dez caminhos
 * para alterá-la sem querer. Mudar significa construir outra e reanexar, que é
 * também o que torna a versão um conceito possível.
 */
public final class Policy {

  private final String nome;
  private final Set<Statement> statements;

  public Policy(String nome, Set<Statement> statements) {
    this.nome = nome;
    this.statements = Collections.unmodifiableSet(new LinkedHashSet<>(statements));
  }

  public String getNome() {
    return nome;
  }

  public Set<Statement> getStatements() {
    return statements;
  }

  @Override
  public boolean equals(Object o) {
    if (!(o instanceof Policy))
      return false;
    var that = (Policy) o;
    return nome.equals(that.nome) && statements.equals(that.statements);
  }

  @Override
  public int hashCode() {
    return Objects.hash(nome, statements);
  }

  @Override
  public String toString() {
    return nome + " (" + statements.size() + " cláusulas)";
  }
}
