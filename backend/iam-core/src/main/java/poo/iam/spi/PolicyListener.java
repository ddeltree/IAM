package poo.iam.spi;

import poo.iam.Permission;
import poo.iam.Principal;

/**
 * Avisa que a política de alguém mudou.
 *
 * Isto existia como um {@code System.out.println} dentro de
 * {@code User.grantPermission}: útil para acompanhar a política sendo montada,
 * e inaceitável numa biblioteca — quem a usasse herdava a saída padrão do
 * núcleo sem ter pedido, e sem ter como calar.
 *
 * O padrão passou a ser o silêncio. Quem quiser o log de antes escreve o
 * ouvinte de uma linha que o imprime; quem quiser auditoria de verdade escreve
 * outro, gravando onde precisar. Nos dois casos a decisão é de quem usa o
 * núcleo, e não dele.
 */
@FunctionalInterface
public interface PolicyListener {

  enum Mudanca {
    CONCEDIDA,
    REVOGADA,
    NEGADA,
    /** A negação explícita saiu — o que não concede nada por si só. */
    NEGACAO_REMOVIDA,
  }

  void politicaMudou(Principal alvo, Mudanca mudanca, Permission permission);

  /** O padrão: uma biblioteca não escreve na saída de ninguém. */
  PolicyListener SILENCIOSO = (alvo, mudanca, permission) -> {
  };
}
