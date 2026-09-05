package poo.iam;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import poo.iam.condition.ConditionJson;

/**
 * Escreve a política de um principal como documento.
 *
 * É o análogo do {@code GetAccountAuthorizationDetails} da AWS, e a razão de
 * ter valido a pena transformar as condições em dado: enquanto elas eram
 * lambdas, a política só existia como código Java e não havia o que imprimir.
 */
public final class PolicyJson {

  private PolicyJson() {
  }

  public static Map<String, Object> deUsuario(User user) {
    return documento(user.getName(), "USUARIO", user.getId(), user.getPolicy());
  }

  public static Map<String, Object> deGrupo(Group grupo) {
    return documento(grupo.getName(), "GRUPO", null, grupo.getPolicy());
  }

  private static Map<String, Object> documento(String nome, String tipo, String id,
      PermissionHolder politica) {
    var doc = new LinkedHashMap<String, Object>();
    doc.put("principal", nome);
    doc.put("tipo", tipo);
    if (id != null)
      doc.put("id", id);
    doc.put("statements", statements(politica));
    return doc;
  }

  private static List<Object> statements(PermissionHolder politica) {
    var res = new ArrayList<Object>();
    for (Statement statement : politica.getStatements()) {
      var item = new LinkedHashMap<String, Object>();
      item.put("sid", statement.getSid());
      item.put("effect", statement.getEffect().name());
      item.put("action", statement.getPermission().getAction().name());
      item.put("resourceType", statement.getPermission().getResourceType().name());
      var condicao = ConditionJson.escrever(statement.getCondition());
      if (condicao != null)
        item.put("condition", condicao);
      res.add(item);
    }
    return res;
  }
}
