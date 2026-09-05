import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.ObjectMapper;

import io.javalin.testtools.JavalinTest;
import poo.console.Main;

/**
 * O console de ponta a ponta.
 *
 * O cenário semente é testado junto, e de propósito: se ele quebrar, a
 * apresentação quebra — melhor descobrir no {@code mvn test} do que na frente
 * de uma sala.
 */
class ConsoleTest {

  private static final ObjectMapper MAPPER = new ObjectMapper();

  @BeforeEach
  void limpar() {
    Main.resetState();
  }

  /**
   * O corpo vai como objeto: o javalin-testtools serializa o que recebe. Passar
   * um RequestBody pronto faria ele serializar o próprio RequestBody, e o
   * backend receberia {"duplex":false,"oneShot":false}.
   */
  private static Object corpo(Object valor) {
    return valor;
  }

  @SuppressWarnings("unchecked")
  private static Map<String, Object> mapa(String json) {
    try {
      return MAPPER.readValue(json, Map.class);
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  // ---------- o cenário semente ----------

  @Test
  void aSementeExercitaCadaCapacidadeDoNucleo() {
    JavalinTest.test(Main.createApp(), (server, client) -> {
      var cenario = mapa(client.get("/cenario").body().string());

      assertEquals(3, ((List<?>) cenario.get("usuarios")).size());
      assertEquals(2, ((List<?>) cenario.get("grupos")).size());
      assertEquals(1, ((List<?>) cenario.get("papeis")).size());
      assertEquals(3, ((List<?>) cenario.get("politicas")).size());
      assertEquals(4, ((List<?>) cenario.get("recursos")).size());
    });
  }

  @Test
  void aPoliticaDeAlguemMostraDeOndeCadaClausulaVeio() {
    JavalinTest.test(Main.createApp(), (server, client) -> {
      var carla = mapa(client.get("/principais/carla").body().string());

      // as três origens aparecem separadas, que é a pergunta "por que ele pode
      // isso?" respondida antes de qualquer simulação
      assertEquals(1, ((List<?>) carla.get("inline")).size(), "o DENY de apagar");
      assertEquals(List.of("leitores"), carla.get("grupos"));
      assertTrue(((List<?>) carla.get("efetiva")).size() > 1, "a soma é maior que o inline");
    });
  }

  // ---------- o simulador ----------

  @Test
  void oSimuladorDizPorQueNaoEmVezDeSoDizerNao() {
    JavalinTest.test(Main.createApp(), (server, client) -> {
      // bruno não é dono do bucket da ana
      var res = mapa(client.post("/simular", corpo(Map.of(
          "principal", "bruno", "acao", "ESCREVER", "tipo", "BUCKET",
          "recurso", "BUCKET/relatorios"))).body().string());

      assertEquals(false, res.get("permitido"));

      var clausulas = (List<Map<String, Object>>) res.get("clausulas");
      assertFalse(clausulas.isEmpty(), "há cláusula falando sobre ESCREVER em BUCKET");
      var doDono = clausulas.stream()
          .filter(c -> "oDonoPodeTudo".equals(c.get("sid"))).findFirst().orElseThrow();
      assertEquals(true, doDono.get("alcancaORecurso"));
      assertEquals(false, doDono.get("condicaoPassou"));

      // e o contexto mostra os dois valores que a condição comparou
      var contexto = (Map<String, Object>) res.get("contexto");
      assertEquals(List.of("ana"), contexto.get("recurso:dono"));
      assertEquals(List.of("bruno"), contexto.get("principal:id"));
    });
  }

  @Test
  void aCorrenteDeRecursosAlcancaOPaiNaSimulacao() {
    JavalinTest.test(Main.createApp(), (server, client) -> {
      // o objeto é da carla, mas está no bucket da ana — e há uma política que
      // concede pelo dono do BUCKET, não do objeto
      var res = mapa(client.post("/simular", corpo(Map.of(
          "principal", "ana", "acao", "LER", "tipo", "OBJETO",
          "recurso", "OBJETO/q1.pdf"))).body().string());

      assertEquals(true, res.get("permitido"));
      assertEquals("peloBucket", ((Map<String, Object>) res.get("decisiva")).get("sid"));

      // a chave do pai aparece no contexto sem ninguém ter navegado à mão
      var contexto = (Map<String, Object>) res.get("contexto");
      assertEquals(List.of("ana"), contexto.get("bucket:dono"));
      assertEquals(List.of("carla"), contexto.get("recurso:dono"));
    });
  }

  @Test
  void aPoliticaDoRecursoConcedeSemTocarNaDeNinguem() {
    JavalinTest.test(Main.createApp(), (server, client) -> {
      var res = mapa(client.post("/simular", corpo(Map.of(
          "principal", "carla", "acao", "LER", "tipo", "BUCKET",
          "recurso", "BUCKET/folha"))).body().string());

      assertEquals(true, res.get("permitido"));
      assertEquals("BUCKET/folha", res.get("origem"), "veio da política do próprio recurso");

      // e a política da carla não tem nada sobre isso
      var carla = mapa(client.get("/principais/carla").body().string());
      assertEquals(1, ((List<?>) carla.get("inline")).size());
    });
  }

  @Test
  void aNegacaoInlineVenceAConcessaoDoGrupo() {
    JavalinTest.test(Main.createApp(), (server, client) -> {
      var res = mapa(client.post("/simular", corpo(Map.of(
          "principal", "carla", "acao", "APAGAR", "tipo", "BUCKET",
          "recurso", "BUCKET/folha"))).body().string());

      assertEquals(false, res.get("permitido"));
      assertEquals("NEGACAO_EXPLICITA", res.get("tipo"));
      assertEquals("carlaNuncaApaga", ((Map<String, Object>) res.get("decisiva")).get("sid"));
    });
  }

  // ---------- consultas reversas ----------

  @Test
  void quemPodeSeparaOsDiretosDosQueChegariamPeloPapel() {
    JavalinTest.test(Main.createApp(), (server, client) -> {
      var res = mapa(client.get("/quem-pode?acao=LER&tipo=BUCKET&recurso=BUCKET/relatorios")
          .body().string());

      var diretos = (List<Map<String, Object>>) res.get("principais");
      assertEquals(List.of("ana"), diretos.stream().map(p -> p.get("id")).toList(),
          "só a dona; o bucket não é público");

      var viaPapel = (Map<String, Object>) res.get("viaPapel");
      assertTrue(viaPapel.containsKey("Auditor"), "os leitores chegariam assumindo o papel");
    });
  }

  @Test
  void ondePossoDevolveOFiltroEOSql() {
    JavalinTest.test(Main.createApp(), (server, client) -> {
      var res = mapa(client.get("/onde-posso?principal=ana&acao=ESCREVER&tipo=BUCKET")
          .body().string());

      assertEquals(List.of("BUCKET/relatorios"), res.get("recursos"));
      assertTrue(String.valueOf(res.get("sql")).contains("recurso.dono = 'ana'"),
          "o filtro derivado da política, em SQL: " + res.get("sql"));
    });
  }

  // ---------- montar do zero, que é o ponto do console ----------

  @Test
  void daParaMontarUmDominioNovoInteiroPelaApi() {
    JavalinTest.test(Main.createApp(), (server, client) -> {
      assertEquals(201, client.post("/vocabulario/permissoes",
          corpo(Map.of("acao", "APROVAR", "tipo", "PEDIDO"))).code());

      assertEquals(201, client.post("/usuarios", corpo(Map.of("nome", "Gerente"))).code());

      assertEquals(200, client.put("/politicas/Aprovacao", corpo(Map.of(
          "statements", List.of(Map.of(
              "sid", "gerenteAprovaAteMil",
              "effect", "ALLOW", "action", "APROVAR", "resource", "PEDIDO",
              "condition", Map.of("MenorOuIgual", Map.of("recurso:valor", List.of("1000")))))))
          ).code());

      assertEquals(204,
          client.post("/principais/gerente/politicas/Aprovacao", corpo(Map.of())).code());

      assertEquals(200, client.put("/recursos/PEDIDO/p1",
          corpo(Map.of("atributos", Map.of("valor", "500")))).code());
      assertEquals(200, client.put("/recursos/PEDIDO/p2",
          corpo(Map.of("atributos", Map.of("valor", "5000")))).code());

      // um domínio que não existia dois minutos atrás, decidindo
      var pequeno = mapa(client.post("/simular", corpo(Map.of(
          "principal", "gerente", "acao", "APROVAR", "tipo", "PEDIDO",
          "recurso", "PEDIDO/p1"))).body().string());
      assertEquals(true, pequeno.get("permitido"));

      var grande = mapa(client.post("/simular", corpo(Map.of(
          "principal", "gerente", "acao", "APROVAR", "tipo", "PEDIDO",
          "recurso", "PEDIDO/p2"))).body().string());
      assertEquals(false, grande.get("permitido"));
    });
  }

  @Test
  void assumirUmPapelTrocaAIdentidade() {
    JavalinTest.test(Main.createApp(), (server, client) -> {
      // a carla não lê o bucket da ana
      var antes = mapa(client.post("/simular", corpo(Map.of(
          "principal", "carla", "acao", "LER", "tipo", "BUCKET",
          "recurso", "BUCKET/relatorios"))).body().string());
      assertEquals(false, antes.get("permitido"));

      var sessao = mapa(client.post("/papeis/auditor/assumir",
          corpo(Map.of("principal", "carla"))).body().string());
      var sessaoId = String.valueOf(sessao.get("id"));

      var depois = mapa(client.post("/simular", corpo(Map.of(
          "principal", sessaoId, "acao", "LER", "tipo", "BUCKET",
          "recurso", "BUCKET/relatorios"))).body().string());
      assertEquals(true, depois.get("permitido"));

      // e principal:id passou a ser o da sessão
      var contexto = (Map<String, Object>) depois.get("contexto");
      assertEquals(List.of(sessaoId), contexto.get("principal:id"));
      assertEquals(List.of("carla"), contexto.get("sessao:origem"));
    });
  }

  @Test
  void umaClausulaRepetidaEAvisadaEmVezDeSumirEmSilencio() {
    JavalinTest.test(Main.createApp(), (server, client) -> {
      var clausula = Map.of("effect", "ALLOW", "action", "LER", "resource", "BUCKET");
      assertEquals(201, client.post("/principais/ana/statements", corpo(clausula)).code());

      // Statement.equals ignora o sid, então a segunda seria engolida pelo Set
      // e a tela mostraria "nada aconteceu"
      var repetida = client.post("/principais/ana/statements", corpo(clausula));
      assertEquals(409, repetida.code());
      assertTrue(repetida.body().string().contains("já existe"));
    });
  }
}
