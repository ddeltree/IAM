package poo.console;

import poo.iam.Action;

/** Uma ação que é só um nome — {@code LER}, {@code s3:GetObject}, o que for. */
public record AcaoLivre(String name) implements Action {

  public AcaoLivre {
    if (name == null || name.isBlank())
      throw new IllegalArgumentException("Uma ação precisa de nome");
  }

  @Override
  public String toString() {
    return name;
  }
}
