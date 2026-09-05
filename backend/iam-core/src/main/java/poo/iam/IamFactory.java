package poo.iam;

import poo.iam.condition.ConditionOperator;
import poo.iam.condition.OperatorRegistry;
import poo.iam.query.PolicyQuery;
import poo.iam.spi.AttributeProvider;
import poo.iam.spi.PrincipalDirectory;

/**
 * Monta um {@link Iam} a partir do que a aplicação fornece.
 *
 * Esta classe é a resposta curta para "o que preciso escrever para usar o
 * núcleo?": tudo que se passa aqui vem de {@code poo.iam.spi}, e nada mais é
 * exigido. Um domínio simples precisa de um {@link AttributeProvider} por tipo
 * de recurso e nada além disso.
 *
 * <pre>
 * var iam = IamFactory.novo()
 *     .atributos(new AtributosDaTurma(), new AtributosDoPost())
 *     .principais(meuDiretorio)
 *     .construir();
 *
 * iam.motor().isAllowed(usuario, EDITAR_POST, post);
 * </pre>
 */
public final class IamFactory {

  private final ContextResolver contexto = new ContextResolver();
  private OperatorRegistry operadores = OperatorRegistry.padrao();
  private PrincipalDirectory diretorio;

  private IamFactory() {
  }

  public static IamFactory novo() {
    return new IamFactory();
  }

  /** Como ler os atributos de cada tipo de recurso do domínio. */
  public IamFactory atributos(AttributeProvider... provedores) {
    for (AttributeProvider provedor : provedores)
      contexto.registrar(provedor);
    return this;
  }

  /**
   * Onde estão os principais. Sem isto o motor decide normalmente, mas
   * {@link Iam#consultas()} vem {@code null}: não dá para responder "quem
   * pode isto?" sem saber quem existe.
   */
  public IamFactory principais(PrincipalDirectory diretorio) {
    this.diretorio = diretorio;
    return this;
  }

  /** Um operador de condição que o núcleo não traz. */
  public IamFactory operador(ConditionOperator operador) {
    if (operadores == OperatorRegistry.padrao())
      operadores = new OperatorRegistry();
    operadores.registrar(operador);
    return this;
  }

  public Iam construir() {
    var motor = new AuthorizationEngine(contexto);
    var consultas = diretorio == null ? null : new PolicyQuery(diretorio, motor);
    return new Iam(motor, consultas, operadores);
  }
}
