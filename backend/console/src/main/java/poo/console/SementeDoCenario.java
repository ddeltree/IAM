package poo.console;

import java.util.List;
import java.util.Map;
import java.util.Set;

import poo.iam.Effect;
import poo.iam.MembershipManager;
import poo.iam.Policy;
import poo.iam.Statement;
import poo.iam.condition.Condition;

/**
 * O cenário com que o console sobe.
 *
 * Ele existe para que a primeira tela não seja um formulário vazio, e é
 * escolhido para exercitar <b>cada</b> capacidade do núcleo de uma vez: grupos,
 * política nomeada anexada, cláusula inline, condição com variável de política,
 * corrente de recursos, política anexada ao próprio recurso, e um papel com
 * política de confiança.
 *
 * O domínio é um serviço de arquivos porque ele é imediatamente compreensível e
 * não é o classroom — o ponto do console é justamente que o núcleo não conhece
 * nenhum domínio.
 */
final class SementeDoCenario {

  private SementeDoCenario() {
  }

  static Cenario montar() {
    var c = new Cenario();

    // ---------- vocabulário ----------
    for (String acao : List.of("LER", "ESCREVER", "APAGAR", "COMPARTILHAR")) {
      c.declararPermissao(acao, "BUCKET", false);
      c.declararPermissao(acao, "OBJETO", false);
    }
    c.declararPermissao("CRIAR_BUCKET", "BUCKET", true); // sem alvo: não há bucket ainda

    // ---------- principais ----------
    var ana = c.criarUsuario("ana", "Ana");
    var bruno = c.criarUsuario("bruno", "Bruno");
    var carla = c.criarUsuario("carla", "Carla");

    var leitores = c.criarGrupo("leitores", "Leitores");
    var escritores = c.criarGrupo("escritores", "Escritores");
    MembershipManager.link(ana, leitores);
    MembershipManager.link(bruno, leitores);
    MembershipManager.link(carla, leitores);
    MembershipManager.link(ana, escritores);
    MembershipManager.link(bruno, escritores);

    // ---------- políticas nomeadas ----------
    // a variável ${principal:id} é o que faz uma cláusula servir a todo mundo
    var donoManda = new Policy("DonoMandaNoSeu", Set.of(
        Statement.de(Effect.ALLOW, "*", "*",
            Condition.igual("recurso:dono", "${principal:id}")).comSid("oDonoPodeTudo")));
    c.salvarPolitica(donoManda);

    var leituraGeral = new Policy("LeituraGeral", Set.of(
        Statement.de(Effect.ALLOW, "LER", "BUCKET",
            Condition.igual("recurso:publico", "true")).comSid("bucketPublicoTodosLeem")));
    c.salvarPolitica(leituraGeral);

    // esta só faz sentido pela corrente: a condição fala do BUCKET, e o recurso
    // avaliado é o OBJETO dentro dele
    var donoDoBucket = new Policy("DonoDoBucketAlcancaOsObjetos", Set.of(
        Statement.de(Effect.ALLOW, "*", "OBJETO",
            Condition.igual("bucket:dono", "${principal:id}")).comSid("peloBucket")));
    c.salvarPolitica(donoDoBucket);

    escritores.anexar(donoManda);
    escritores.anexar(donoDoBucket);
    leitores.anexar(leituraGeral);

    // uma cláusula inline, para a tela mostrar as três origens lado a lado
    carla.add(Statement.de(Effect.DENY, "APAGAR", "*", Condition.SEMPRE)
        .comSid("carlaNuncaApaga"));

    // ---------- recursos ----------
    var relatorios = new RecursoLivre("BUCKET", "relatorios");
    relatorios.setAtributos(Map.of("dono", List.of("ana"), "publico", List.of("false")));
    c.salvarRecurso(relatorios);

    var publico = new RecursoLivre("BUCKET", "manuais");
    publico.setAtributos(Map.of("dono", List.of("bruno"), "publico", List.of("true")));
    c.salvarRecurso(publico);

    var folha = new RecursoLivre("BUCKET", "folha");
    folha.setAtributos(Map.of("dono", List.of("bruno"), "publico", List.of("false")));
    // a bucket policy: o dono compartilha sem tocar na política de ninguém
    folha.setPolitica(new Policy("folha", Set.of(
        Statement.de(Effect.ALLOW, "LER", "BUCKET/folha",
            Condition.igual("principal:id", "carla")).comSid("compartilhadoComCarla"))));
    c.salvarRecurso(folha);

    var q1 = new RecursoLivre("OBJETO", "q1.pdf");
    q1.setAtributos(Map.of("dono", List.of("carla")));
    q1.setPaiRef("BUCKET/relatorios"); // a corrente
    c.salvarRecurso(q1);

    // ---------- papel ----------
    var auditor = c.criarPapel("auditor", "Auditor");
    auditor.add(Statement.de(Effect.ALLOW, "LER", "*", Condition.SEMPRE).comSid("auditorLeTudo"));
    auditor.confiaEm(Condition.contem("principal:groups", "Leitores"));

    return c;
  }
}
