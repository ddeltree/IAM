package poo.iam.document;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Set;

import poo.iam.ActionPattern;
import poo.iam.Effect;
import poo.iam.Group;
import poo.iam.Policy;
import poo.iam.ResourcePattern;
import poo.iam.Statement;
import poo.iam.User;
import poo.iam.condition.OperatorRegistry;

/**
 * A política como documento, nos dois sentidos.
 *
 * Escrever é o análogo do {@code GetAccountAuthorizationDetails} da AWS, e foi
 * a primeira razão de as condições terem virado dado: enquanto eram lambdas, a
 * política só existia como código Java e não havia o que imprimir.
 *
 * Ler é a outra metade, e sem ela a primeira vale menos do que parece. Uma
 * política que só se escreve continua morando no código: para mudar quem pode o
 * quê é preciso recompilar, e o documento é um relatório, não a fonte. Com a
 * volta, o código passa a ser o motor e o documento passa a ser a política.
 */
public final class PolicyDocument {

  private PolicyDocument() {
  }

  public static Map<String, Object> deUsuario(User user) {
    return documento(user.getName(), "USUARIO", user.getId(), user.getStatements());
  }

  public static Map<String, Object> deGrupo(Group grupo) {
    return documento(grupo.getName(), "GRUPO", null, grupo.getStatements());
  }

  // as cláusulas chegam pelo getter público, e não pelo PermissionHolder: a
  // política é detalhe interno do principal, o documento só precisa lê-la
  private static Map<String, Object> documento(String nome, String tipo, String id,
      Set<Statement> politica) {
    var doc = new LinkedHashMap<String, Object>();
    doc.put("principal", nome);
    doc.put("tipo", tipo);
    if (id != null)
      doc.put("id", id);
    doc.put("statements", statements(politica));
    return doc;
  }

  // ---------- políticas nomeadas ----------

  public static Map<String, Object> escrever(Policy policy) {
    var doc = new LinkedHashMap<String, Object>();
    doc.put("nome", policy.getNome());
    doc.put("statements", statements(policy.getStatements()));
    return doc;
  }

  /** {@code { "politicas": [ {...}, {...} ] }} */
  public static Map<String, Object> escreverTodas(Collection<Policy> politicas) {
    return Map.of("politicas", politicas.stream().map(PolicyDocument::escrever).toList());
  }

  public static Policy ler(Object node) {
    return ler(node, OperatorRegistry.padrao());
  }

  public static Policy ler(Object node, OperatorRegistry operadores) {
    var mapa = objeto(node, "uma política");
    var nome = texto(mapa.get("nome"));
    if (nome == null)
      throw new IllegalArgumentException("Política sem nome: " + mapa.keySet());

    var statements = new LinkedHashSet<Statement>();
    for (Object item : lista(mapa.get("statements"), "statements"))
      statements.add(lerStatement(item, operadores));
    return new Policy(nome, statements);
  }

  public static List<Policy> lerTodas(Object node) {
    return lerTodas(node, OperatorRegistry.padrao());
  }

  public static List<Policy> lerTodas(Object node, OperatorRegistry operadores) {
    var mapa = objeto(node, "um documento de políticas");
    var res = new ArrayList<Policy>();
    for (Object item : lista(mapa.get("politicas"), "politicas"))
      res.add(ler(item, operadores));
    return res;
  }

  public static Statement lerStatement(Object node, OperatorRegistry operadores) {
    var mapa = objeto(node, "uma cláusula");
    var effect = texto(mapa.get("effect"));
    var action = texto(mapa.get("action"));
    var resource = texto(mapa.get("resource"));
    if (effect == null || action == null)
      throw new IllegalArgumentException("Cláusula sem effect ou action: " + mapa);

    return Statement.de(
        Effect.valueOf(effect),
        ActionPattern.de(action),
        ResourcePattern.de(resource == null ? "*" : resource),
        ConditionDocument.ler(mapa.get("condition"), operadores),
        texto(mapa.get("sid")));
  }

  // ---------- leitura defensiva ----------
  //
  // Um documento malformado precisa falhar dizendo o que está errado. A
  // alternativa — ignorar o campo que não entendeu — produziria uma política
  // silenciosamente mais permissiva ou mais restritiva que a escrita, e é o
  // tipo de erro que só aparece quando alguém não consegue fazer o trabalho.

  private static Map<?, ?> objeto(Object node, String oQue) {
    if (!(node instanceof Map))
      throw new IllegalArgumentException("Esperava " + oQue + " e veio "
          + (node == null ? "nada" : node.getClass().getSimpleName()));
    return (Map<?, ?>) node;
  }

  private static List<?> lista(Object node, String campo) {
    if (node == null)
      return List.of();
    if (!(node instanceof List))
      throw new IllegalArgumentException("O campo " + campo + " precisa ser uma lista");
    return (List<?>) node;
  }

  private static String texto(Object valor) {
    return valor == null ? null : String.valueOf(valor);
  }

  private static List<Object> statements(Set<Statement> politica) {
    var res = new ArrayList<Object>();
    for (Statement statement : politica) {
      var item = new LinkedHashMap<String, Object>();
      item.put("sid", statement.getSid());
      item.put("effect", statement.getEffect().name());
      item.put("action", statement.getAction().toString());
      item.put("resource", statement.getResource().toString());
      var condicao = ConditionDocument.escrever(statement.getCondition());
      if (condicao != null)
        item.put("condition", condicao);
      res.add(item);
    }
    return res;
  }
}
