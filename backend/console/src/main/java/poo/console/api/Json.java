package poo.console.api;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import poo.console.Cenario;
import poo.iam.Permission;
import poo.iam.Policy;
import poo.iam.Principal;
import poo.iam.Statement;
import poo.iam.document.PolicyDocument;

/**
 * Como cada coisa do núcleo vira JSON para esta tela.
 *
 * Fica num lugar só porque a mesma cláusula aparece em cinco telas — na
 * política, no principal, no simulador, na consulta reversa e no recurso — e
 * três formatos diferentes para a mesma coisa é como a interface acaba com três
 * componentes que fazem o mesmo.
 */
public final class Json {

  private Json() {
  }

  public static Map<String, Object> statement(Statement s) {
    var m = new LinkedHashMap<String, Object>();
    m.put("sid", s.getSid());
    m.put("effect", s.getEffect().name());
    m.put("action", s.getAction().toString());
    m.put("resource", s.getResource().toString());
    var condicao = poo.iam.document.ConditionDocument.escrever(s.getCondition());
    m.put("condition", condicao); // null quando irrestrita: a tela mostra "sempre"
    return m;
  }

  public static List<Object> statements(Iterable<Statement> statements) {
    var res = new ArrayList<Object>();
    statements.forEach(s -> res.add(statement(s)));
    return res;
  }

  public static Map<String, Object> politica(Policy p) {
    return Map.of("nome", p.getNome(), "statements", statements(p.getStatements()));
  }

  /**
   * A política com quem a tem anexada.
   *
   * Fica aqui, e não só no controller de políticas, porque a tela lê a lista do
   * cenário inteiro — e dois endpoints devolvendo formas diferentes para a
   * mesma coisa é como um campo some sem ninguém notar.
   */
  public static Map<String, Object> politicaComAnexos(Policy p, Cenario cenario) {
    var m = new LinkedHashMap<String, Object>(politica(p));
    var quem = new ArrayList<String>();
    cenario.usuarios().forEach(u -> {
      if (u.getPoliticasAnexadas().contains(p))
        quem.add(u.getId());
    });
    cenario.grupos().forEach(g -> {
      if (g.getPoliticasAnexadas().contains(p))
        quem.add(g.getId());
    });
    m.put("anexadaA", quem);
    return m;
  }

  public static Map<String, Object> permissao(Permission p) {
    return Map.of("acao", p.getAction().name(), "tipo", p.getResourceType().name(),
        "rotulo", p.getAction().name() + " · " + p.getResourceType().name());
  }

  /**
   * Um principal com a política dele <b>separada por origem</b>.
   *
   * Somar tudo numa lista só seria mais simples e esconderia a coisa mais
   * importante da tela: de onde cada permissão vem. "Por que ele pode isso?" se
   * responde aqui, antes de qualquer simulação.
   */
  public static Map<String, Object> principal(Principal p, Cenario cenario) {
    var m = new LinkedHashMap<String, Object>();
    m.put("id", p.getId());
    m.put("nome", p.getName());
    m.put("tipo", tipoDe(p));

    if (p instanceof poo.iam.User user) {
      m.put("inline", statements(user.getStatementsInline()));
      m.put("anexadas", user.getPoliticasAnexadas().stream().map(Policy::getNome).toList());
      m.put("grupos", user.getGroups().stream().map(g -> g.getId()).toList());
    } else if (p instanceof poo.iam.Group grupo) {
      m.put("inline", statements(grupo.getStatementsInline()));
      m.put("anexadas", grupo.getPoliticasAnexadas().stream().map(Policy::getNome).toList());
      m.put("membros", grupo.getUsers().stream().map(u -> u.getId()).toList());
    } else if (p instanceof poo.iam.Role papel) {
      m.put("inline", statements(papel.getStatements()));
      m.put("confianca", statements(papel.getConfiancaStatements()));
    } else if (p instanceof poo.iam.Session sessao) {
      m.put("papel", sessao.getPapel().getId());
      m.put("origem", sessao.getOrigem().getId());
      m.put("inline", List.of());
    }

    // e a soma, que é o que o motor de fato percorre — com a origem de cada
    // cláusula, porque uma lista achatada esconde justamente o que a pergunta
    // "por que ele pode isso?" quer saber
    var efetiva = new ArrayList<Object>();
    for (var c : cenario.iam().motor().clausulasDe(p)) {
      var linha = new LinkedHashMap<String, Object>(statement(c.statement()));
      linha.put("origem", c.origem());
      efetiva.add(linha);
    }
    m.put("efetiva", efetiva);
    return m;
  }

  public static String tipoDe(Principal p) {
    if (p instanceof poo.iam.User)
      return "USUARIO";
    if (p instanceof poo.iam.Group)
      return "GRUPO";
    if (p instanceof poo.iam.Role)
      return "PAPEL";
    if (p instanceof poo.iam.Session)
      return "SESSAO";
    return "DESCONHECIDO";
  }

  public static Map<String, Object> documentoDePoliticas(Cenario cenario) {
    return PolicyDocument.escreverTodas(cenario.politicas());
  }
}
