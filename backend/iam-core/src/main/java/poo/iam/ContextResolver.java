package poo.iam;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

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

  private static final ContextResolver PADRAO = new ContextResolver();

  private final Map<ResourceType, AttributeProvider> provedores = new HashMap<>();

  public static ContextResolver padrao() {
    return PADRAO;
  }

  public void registrar(AttributeProvider provedor) {
    provedores.put(provedor.tipo(), provedor);
  }

  /** Esquece os provedores. Existe para os testes reiniciarem o estado. */
  public void limpar() {
    provedores.clear();
  }

  public RequestContext resolver(User principal, Resource alvo, Object... extra) {
    var valores = new LinkedHashMap<String, List<String>>();

    if (principal != null) {
      valores.put("principal:id", List.of(principal.getId()));
      if (principal.getName() != null)
        valores.put("principal:name", List.of(principal.getName()));
      valores.put("principal:groups", nomesDosGrupos(principal));
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

    return new RequestContext(principal, alvo, valores, extra);
  }

  private Map<String, List<String>> atributosDe(Resource recurso) {
    var provedor = provedores.get(recurso.getType());
    if (provedor == null)
      return Map.of();
    return provedor.atributosDe(recurso);
  }

  private static List<String> nomesDosGrupos(User principal) {
    var nomes = new ArrayList<String>();
    for (Group grupo : principal.getGroups())
      nomes.add(grupo.getName());
    return nomes;
  }
}
