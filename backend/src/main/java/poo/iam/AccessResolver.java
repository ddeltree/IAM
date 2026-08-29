package poo.iam;

/**
 * O motor de decisão do núcleo — a única peça que sabe combinar política inline
 * com política de grupo.
 *
 * Segue a mesma ordem da AWS: negação explícita vence qualquer concessão, e o
 * padrão, na ausência de ambas, é negar.
 */
public final class AccessResolver {

  private static final String INLINE = "inline";

  private AccessResolver() {
  }

  public static boolean isAllowed(User user, Permission permission, Resource resource, Object... context) {
    return avaliar(user, permission, resource, context).permitido();
  }

  /**
   * Como {@link #isAllowed}, mas devolve também qual cláusula decidiu e onde
   * ela estava. É o que sustenta a explicação de um 403.
   */
  public static Decisao avaliar(User user, Permission permission, Resource resource, Object... context) {
    if (user == null)
      return Decisao.negacaoPadrao("nenhum usuário autenticado");

    // Sem esta conferência uma concessão sem condição alcançaria qualquer
    // objeto: o administrador recebe EDITAR_POST irrestrito, e nada impediria
    // essa cláusula de valer sobre uma turma. Recurso nulo é permitido de
    // propósito — é como se pedem ações que não têm alvo, como criar uma turma.
    if (resource != null && !resource.getType().equals(permission.getResourceType()))
      return Decisao.negacaoPadrao(
          permission + " não se aplica a um recurso do tipo " + resource.getType().name());

    var negacao = procurar(user, permission, resource, true, context);
    if (negacao != null)
      return Decisao.negacaoExplicita(negacao.statement, negacao.origem);

    var concessao = procurar(user, permission, resource, false, context);
    if (concessao != null)
      return Decisao.permitido(concessao.statement, concessao.origem);

    return Decisao.negacaoPadrao(
        "nenhuma cláusula concede " + permission + " a " + user.getName());
  }

  /** Varre a política inline e depois a de cada grupo, na mesma ordem sempre. */
  private static Achado procurar(User user, Permission permission, Resource resource,
      boolean negacao, Object... context) {
    var inline = negacao
        ? user.getPolicy().negacaoQueAplica(permission, user, resource, context)
        : user.getPolicy().concessaoQueAplica(permission, user, resource, context);
    if (inline != null)
      return new Achado(inline, INLINE);

    for (Group group : user.getGroups()) {
      var doGrupo = negacao
          ? group.getPolicy().negacaoQueAplica(permission, user, resource, context)
          : group.getPolicy().concessaoQueAplica(permission, user, resource, context);
      if (doGrupo != null)
        return new Achado(doGrupo, group.getName());
    }
    return null;
  }

  private static final class Achado {
    final Statement statement;
    final String origem;

    Achado(Statement statement, String origem) {
      this.statement = statement;
      this.origem = origem;
    }
  }
}
