package poo.iam;

/**
 * O motor de decisão do núcleo — a única peça que sabe combinar política inline
 * com política de grupo.
 *
 * Segue a mesma ordem da AWS: negação explícita vence qualquer concessão, e o
 * padrão, na ausência de ambas, é negar.
 */
public final class AccessResolver {

  private AccessResolver() {
  }

  public static boolean isAllowed(User user, Permission permission, Resource resource, Object... context) {
    if (user == null)
      return false;
    if (nega(user, permission, resource, context))
      return false;
    return permite(user, permission, resource, context);
  }

  private static boolean nega(User user, Permission permission, Resource resource, Object... context) {
    if (user.getPolicy().nega(permission, user, resource, context))
      return true;
    for (Group group : user.getGroups()) {
      if (group.getPolicy().nega(permission, user, resource, context))
        return true;
    }
    return false;
  }

  private static boolean permite(User user, Permission permission, Resource resource, Object... context) {
    if (user.getPolicy().permite(permission, user, resource, context))
      return true;
    for (Group group : user.getGroups()) {
      if (group.getPolicy().permite(permission, user, resource, context))
        return true;
    }
    return false;
  }
}
