package poo.console;

import poo.iam.ResourceType;

/**
 * Um tipo de recurso que é só um nome, criado quando alguém o digita na tela.
 *
 * O núcleo compara {@link ResourceType} por nome, e não por objeto — foi
 * justamente para isto: um vocabulário aberto não pode exigir um enum, porque
 * enum é escrito antes de o programa rodar.
 */
public record TipoLivre(String name) implements ResourceType {

  public TipoLivre {
    if (name == null || name.isBlank())
      throw new IllegalArgumentException("Um tipo de recurso precisa de nome");
  }

  @Override
  public String toString() {
    return name;
  }
}
