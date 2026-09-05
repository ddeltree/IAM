package poo.iam.spi;

import java.util.Collection;
import java.util.List;

import poo.iam.Permission;

/**
 * Tudo o que se pode pedir nesta aplicação.
 *
 * O núcleo sabe decidir sobre uma permissão que lhe apresentem, mas não sabe
 * quais existem — e sem isso não há como responder "o que este usuário pode
 * fazer aqui?", que é a pergunta de que uma interface precisa para esconder um
 * botão. Enumerar exige uma lista do que existe; a decisão, não.
 *
 * Ficou obrigatório quando os padrões de ação entraram. Uma cláusula
 * {@code allow("*", "*")} concede um conjunto que só se enumera contra um
 * catálogo: sem ele, curinga seria fácil de conceder e impossível de consultar,
 * e a consultabilidade é justamente o que não se pode perder.
 *
 * <h2>O risco de esquecer uma</h2>
 *
 * Uma ação que os controllers pedem mas que não está aqui continua sendo
 * concedida e decidida normalmente — ela só some das respostas de "o que posso
 * fazer". Ou seja, a interface esconde um botão que funcionaria. Não há como o
 * núcleo detectar isso; quem consegue é um teste da aplicação, conferindo que
 * toda permissão pedida em algum lugar está no catálogo.
 */
public interface ActionCatalog {

  /** Todas as permissões, na ordem em que se quer vê-las. */
  Collection<Permission> todas();

  /**
   * As que se pedem sem alvo — criar uma turma, listar usuários.
   *
   * Perguntar "posso criar uma turma <em>nesta</em> turma?" não é uma
   * pergunta, e é esta divisão que evita a resposta sem sentido.
   */
  default Collection<Permission> semAlvo() {
    return List.of();
  }
}
