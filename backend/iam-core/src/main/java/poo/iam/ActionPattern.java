package poo.iam;

import java.util.Objects;

/**
 * A ação que uma cláusula alcança, possivelmente com curinga — o campo
 * {@code Action} da AWS, onde se escreve {@code "s3:GetObject"},
 * {@code "s3:Get*"} ou {@code "*"}.
 *
 * Sem isto, dizer "pode tudo" custa uma cláusula por ação: o administrador
 * desta aplicação precisava de quinze linhas para exprimir uma ideia só, e cada
 * ação nova exigia lembrar de acrescentá-lo. Pior que a repetição é o silêncio
 * do esquecimento — a política fica errada sem nada acusar.
 *
 * <h2>O preço, e como ele é pago</h2>
 *
 * Curinga é fácil de conceder e difícil de enumerar: perguntado "o que este
 * usuário pode fazer aqui?", ninguém consegue expandir {@code "*"} sem uma
 * lista do que existe. É por isso que {@link poo.iam.spi.ActionCatalog} passou
 * a existir junto — a expressividade nova vem com a peça que a mantém
 * consultável, e não depois dela.
 */
public final class ActionPattern {

  public static final ActionPattern TUDO = new ActionPattern("*");

  private final String padrao;

  private ActionPattern(String padrao) {
    this.padrao = padrao;
  }

  public static ActionPattern de(String padrao) {
    return new ActionPattern(padrao);
  }

  public static ActionPattern de(Action action) {
    return new ActionPattern(action.name());
  }

  public boolean casa(Action action) {
    return casa(action.name());
  }

  public boolean casa(String nome) {
    return Curinga.casa(nome, padrao);
  }

  /** Alcança exatamente uma ação, sem curinga? */
  public boolean exato() {
    return padrao.indexOf('*') < 0 && padrao.indexOf('?') < 0;
  }

  public String getPadrao() {
    return padrao;
  }

  @Override
  public boolean equals(Object o) {
    return o instanceof ActionPattern && padrao.equals(((ActionPattern) o).padrao);
  }

  @Override
  public int hashCode() {
    return Objects.hash(padrao);
  }

  @Override
  public String toString() {
    return padrao;
  }
}
