package poo.iam;

/**
 * Quem entrega uma {@link Session}, se a política de confiança do papel
 * permitir — o {@code sts:AssumeRole} da AWS.
 *
 * Não há avaliação nova aqui. A política de confiança <em>é</em> uma política
 * de recurso, com o papel como recurso e {@code ASSUMIR_PAPEL} como ação, então
 * decidir quem pode assumir é o motor de sempre respondendo uma pergunta de
 * sempre. Foi a razão de a Etapa E vir antes desta: sem política no recurso,
 * isto precisaria de um caminho de decisão próprio, e um segundo caminho de
 * decisão é um segundo lugar onde errar.
 */
public final class SessionBroker {

  private static final Permission ASSUMIR =
      new Permission(IamAction.ASSUMIR_PAPEL, PrincipalResource.PAPEL);

  private final AuthorizationEngine motor;
  private long proximaSessao = 1;

  public SessionBroker(AuthorizationEngine motor) {
    this.motor = motor;
  }

  public Decisao podeAssumir(Principal quem, Role papel) {
    // o motor de confiança enxerga só uma política de recurso: a do papel
    var deConfianca = new AuthorizationEngine(motor.getContexto(),
        recurso -> recurso == papel ? papel.getConfianca() : null);
    return deConfianca.avaliar(quem, ASSUMIR, papel);
  }

  /**
   * @return a sessão, ou {@code null} se a confiança do papel não alcançar
   *         quem pediu. Use {@link #podeAssumir} quando o motivo importar.
   */
  public Session assumir(Principal quem, Role papel) {
    return assumir(quem, papel, null);
  }

  public Session assumir(Principal quem, Role papel, String nomeDaSessao) {
    if (!podeAssumir(quem, papel).permitido())
      return null;
    var id = nomeDaSessao != null ? nomeDaSessao
        : papel.getId() + ":" + quem.getId() + ":" + proximaSessao++;
    return new Session(id, papel, quem);
  }
}
