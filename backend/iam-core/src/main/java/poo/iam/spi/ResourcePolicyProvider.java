package poo.iam.spi;

import java.util.Collection;
import java.util.List;

import poo.iam.Policy;
import poo.iam.Resource;
import poo.iam.ResourceType;

/**
 * A política anexada ao próprio recurso — a <em>bucket policy</em> da AWS.
 *
 * É o eixo que falta quando só existe política de identidade: compartilhar uma
 * turma com um professor convidado obrigaria a editar a política <em>dele</em>,
 * e a de todo mundo que já a tem. Com política no recurso, o dono do objeto
 * concede sobre o objeto, e nada em quem recebe precisa mudar.
 *
 * <h2>Quem ela alcança</h2>
 *
 * Não há campo {@code Principal} no {@link poo.iam.Statement}. Não precisa
 * haver: as condições já leem {@code principal:id} e {@code principal:groups},
 * então "o professor 7 pode ver esta turma" se escreve
 * {@code Igual {"principal:id": "7"}}. Um campo novo diria a mesma coisa com
 * uma regra a mais para o motor, para o documento e para as duas extrações de
 * restrição entenderem.
 *
 * <h2>Ordem de avaliação</h2>
 *
 * Simplificando a da AWS para conta única: <b>negação explícita em qualquer uma
 * das políticas vence; depois basta uma concessão, de identidade ou de recurso;
 * na ausência das duas, nega</b>. A AWS exige concessão nos dois lados quando as
 * contas diferem, o que aqui não teria sentido.
 */
public interface ResourcePolicyProvider {

  /** A política deste recurso, ou {@code null} se ele não tiver uma. */
  Policy politicaDe(Resource recurso);

  /**
   * Os recursos deste tipo que têm política própria.
   *
   * Serve à pergunta ao contrário. "Sobre o que posso agir?" se responde
   * derivando um filtro da política de identidade — mas uma concessão que mora
   * no recurso não aparece em filtro nenhum: ela não é uma regra sobre
   * atributos, é uma lista. Sem esta enumeração, os recursos compartilhados
   * sumiriam da resposta, que é o modo mais silencioso de perder
   * consultabilidade.
   *
   * O padrão devolve vazio: uma aplicação que não indexe isso continua
   * funcionando, e paga com {@code onde-posso} incompleto — o que está dito
   * aqui em vez de descoberto depois.
   */
  default Collection<Resource> comPoliticaPropria(ResourceType tipo) {
    return List.of();
  }
}
