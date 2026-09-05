package poo.iam;

import poo.iam.condition.OperatorRegistry;
import poo.iam.query.PolicyQuery;

/**
 * Um componente de autorização montado e pronto para uso.
 *
 * Junta as peças que precisam concordar entre si: o motor decide lendo o mesmo
 * contexto que as consultas usam para podar, e as duas coisas resolvem nomes de
 * operador no mesmo registro. Enquanto isso era tudo estático, a concordância
 * era acidente de haver um sistema só no processo.
 *
 * Monte com {@link IamFactory}.
 */
public final class Iam {

  private final AuthorizationEngine motor;
  private final PolicyQuery consultas;
  private final EffectivePermissions efetivas;
  private final OperatorRegistry operadores;

  Iam(AuthorizationEngine motor, PolicyQuery consultas, EffectivePermissions efetivas,
      OperatorRegistry operadores) {
    this.motor = motor;
    this.consultas = consultas;
    this.efetivas = efetivas;
    this.operadores = operadores;
  }

  /** "Este principal pode esta ação neste recurso?" */
  public AuthorizationEngine motor() {
    return motor;
  }

  /**
   * As perguntas ao contrário: quem pode isto, sobre o que posso agir.
   *
   * {@code null} quando nenhum {@link poo.iam.spi.PrincipalDirectory} foi
   * fornecido — sem saber onde estão os principais, não há como enumerá-los.
   */
  public PolicyQuery consultas() {
    return consultas;
  }

  /**
   * "O que este principal pode fazer aqui?", em lote.
   *
   * {@code null} quando nenhum {@link poo.iam.spi.ActionCatalog} foi fornecido
   * — sem saber o que existe, não há o que enumerar.
   */
  public EffectivePermissions efetivas() {
    return efetivas;
  }

  /** Entrega sessões de papéis assumidos. */
  public SessionBroker papeis() {
    return new SessionBroker(motor);
  }

  public ContextResolver contexto() {
    return motor.getContexto();
  }

  public OperatorRegistry operadores() {
    return operadores;
  }
}
