package poo.iam;

import java.util.Collection;

/**
 * De onde o motor tira a lista de principais quando a pergunta é ao contrário
 * — "quem pode isto?".
 *
 * O núcleo não guarda usuários; quem os guarda é a aplicação. Esta interface é
 * o que permite consultar sem inverter a dependência.
 */
public interface PrincipalDirectory {

  Collection<User> usuarios();

  Collection<Group> grupos();
}
