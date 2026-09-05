package poo.iam.spi;

import java.util.Collection;
import java.util.List;

import poo.iam.Group;
import poo.iam.Role;
import poo.iam.User;

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

  /**
   * Os papéis que se pode assumir.
   *
   * O padrão é vazio: uma aplicação sem papéis não precisa saber que eles
   * existem, e as consultas simplesmente não terão o que reportar por essa via.
   */
  default Collection<Role> papeis() {
    return List.of();
  }
}
