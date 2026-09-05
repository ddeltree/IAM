package poo.iam;

import java.util.Collection;
import java.util.List;
import java.util.Set;

/**
 * Quem pede autorização.
 *
 * O núcleo não precisa que isso seja a classe {@link User} dele: precisa de um
 * identificador, de um nome e de um conjunto de cláusulas. Uma aplicação com
 * usuários vindos de um banco, de um LDAP ou de um token implementa esta
 * interface sobre o que já tem, em vez de herdar da classe daqui.
 *
 * <h2>Herança de política</h2>
 *
 * {@link #herdaDe()} é o que faz um usuário receber as permissões dos grupos
 * dele — e é deliberadamente genérico. O motor não sabe o que é um grupo: ele
 * percorre o grafo de principais somando políticas. Foi assim que grupos,
 * papéis assumidos e sessões passaram a caber no mesmo algoritmo, em vez de
 * cada um virar um caso especial dentro do avaliador.
 *
 * O grafo pode ter ciclo — nada impede um grupo de herdar de outro que herda
 * dele —, e por isso quem o percorre carrega um conjunto de visitados.
 */
public interface Principal {

  String getId();

  String getName();

  /** As cláusulas próprias deste principal, sem as herdadas. */
  Set<Statement> getStatements();

  /** Os principais cujas políticas também valem para este. */
  default Collection<? extends Principal> herdaDe() {
    return List.of();
  }
}
