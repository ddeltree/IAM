package poo.iam;

import java.util.regex.Pattern;

/**
 * O casamento com {@code *} e {@code ?} usado por toda parte: nos padrões de
 * ação, nos de recurso e no operador {@code Parecido} das condições.
 *
 * Estava escrito duas vezes; ficar em um lugar só é o que garante que
 * {@code "EDITAR_*"} signifique a mesma coisa nos três.
 */
public final class Curinga {

  private Curinga() {
  }

  public static boolean casa(String valor, String padrao) {
    if (padrao.equals("*"))
      return true;
    if (padrao.indexOf('*') < 0 && padrao.indexOf('?') < 0)
      return valor.equals(padrao);
    return valor.matches(regex(padrao));
  }

  private static String regex(String padrao) {
    var regex = new StringBuilder("^");
    for (char c : padrao.toCharArray()) {
      switch (c) {
        case '*' -> regex.append(".*");
        case '?' -> regex.append('.');
        default -> regex.append(Pattern.quote(String.valueOf(c)));
      }
    }
    return regex.append('$').toString();
  }
}
