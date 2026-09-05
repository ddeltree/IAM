package poo.iam;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.temporal.ChronoUnit;

import poo.iam.spi.AttributeProvider;

/**
 * Monta o {@link RequestContext} de um pedido a partir dos provedores que a
 * aplicação registrou.
 *
 * Sobe a corrente de {@link Resource#getPai()} publicando as chaves de cada
 * nível com o tipo na frente — {@code post:autorId}, {@code turma:professorId}
 * —, o que é o que permite uma condição sobre a turma ser avaliada partindo de
 * um comentário. As chaves do recurso-alvo aparecem também como
 * {@code recurso:*}, para que uma regra como "o autor pode editar" sirva a
 * post, atividade e comentário sem precisar de uma variante por tipo.
 */
public final class ContextResolver {

  private final Map<ResourceType, AttributeProvider> provedores = new HashMap<>();
  private Clock relogio = Clock.systemDefaultZone();

  public void registrar(AttributeProvider provedor) {
    provedores.put(provedor.tipo(), provedor);
  }

  /** Esquece os provedores registrados. */
  public void limpar() {
    provedores.clear();
  }

  /**
   * De onde vem a hora publicada em {@code contexto:*}.
   *
   * Existe para poder ser fixado: uma política que só vale durante a aula é
   * intestável se o relógio for o do sistema.
   */
  public ContextResolver comRelogio(Clock relogio) {
    this.relogio = relogio == null ? Clock.systemDefaultZone() : relogio;
    return this;
  }

  public RequestContext resolver(Principal principal, Resource alvo) {
    return resolver(principal, alvo, Map.of());
  }

  /**
   * @param chavesDaRequisicao o que só o chamador sabe — o IP de origem, o
   *        método HTTP, um cabeçalho. É o equivalente às chaves {@code aws:*}
   *        que o serviço acrescenta ao pedido, e o lugar por onde uma condição
   *        alcança dados que não estão nem no principal nem no recurso.
   */
  public RequestContext resolver(Principal principal, Resource alvo,
      Map<String, List<String>> chavesDaRequisicao) {
    var valores = new LinkedHashMap<String, List<String>>();

    // primeiro as do chamador, para que as do núcleo não possam ser forjadas:
    // putIfAbsent adiante faz as nossas perderem, então elas entram por último
    if (chavesDaRequisicao != null)
      chavesDaRequisicao.forEach((chave, valor) -> valores.put("requisicao:" + chave, valor));

    var agora = OffsetDateTime.now(relogio).truncatedTo(ChronoUnit.SECONDS);
    valores.put("contexto:instante", List.of(agora.toString()));
    valores.put("contexto:data", List.of(agora.toLocalDate().toString()));
    valores.put("contexto:hora", List.of(agora.toLocalTime().toString()));

    if (principal != null) {
      valores.put("principal:id", List.of(principal.getId()));
      if (principal.getName() != null)
        valores.put("principal:name", List.of(principal.getName()));
      valores.put("principal:groups", nomesDeQuemHerda(principal));

      // as do próprio principal entram por último e com putIfAbsent: as do
      // núcleo vencem, e ninguém forja o próprio id
      principal.chavesDeContexto().forEach(valores::putIfAbsent);
    }

    for (Resource atual = alvo; atual != null; atual = atual.getPai()) {
      var prefixo = atual.getType().name().toLowerCase() + ":";
      var atributos = atributosDe(atual);
      atributos.forEach((chave, valor) -> valores.putIfAbsent(prefixo + chave, valor));

      // o alvo ganha um apelido genérico, para regras que valem em vários tipos
      if (atual == alvo) {
        atributos.forEach((chave, valor) -> valores.putIfAbsent("recurso:" + chave, valor));
        valores.putIfAbsent("recurso:id", List.of(atual.getId()));
        valores.putIfAbsent("recurso:tipo", List.of(atual.getType().name()));
      }
    }

    return new RequestContext(principal, alvo, valores);
  }

  private Map<String, List<String>> atributosDe(Resource recurso) {
    var provedor = provedores.get(recurso.getType());
    if (provedor == null)
      return Map.of();
    return provedor.atributosDe(recurso);
  }

  /**
   * Os nomes dos principais de quem este herda — para um usuário, os grupos
   * dele. A chave continua se chamando {@code principal:groups} porque é assim
   * que as políticas já escritas se referem a ela.
   */
  private static List<String> nomesDeQuemHerda(Principal principal) {
    var nomes = new ArrayList<String>();
    for (Principal herdado : principal.herdaDe())
      nomes.add(herdado.getName());
    return nomes;
  }
}
