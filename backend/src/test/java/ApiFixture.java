import static org.junit.jupiter.api.Assertions.assertEquals;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.Map;

import io.javalin.testtools.HttpClient;
import io.javalin.testtools.JavalinTest;
import io.javalin.testtools.TestCase;
import io.javalin.testtools.TestConfig;
import okhttp3.Response;
import poo.Main;

/**
 * Base dos testes de API: sobe a aplicação já limpa e concentra os helpers
 * HTTP.
 *
 * A autenticação é o cookie {@code uid}, então cada helper recebe o id de quem
 * está fazendo a chamada.
 */
public abstract class ApiFixture {

  public static final int ADM_ID = 1;
  public static final int PROF1_ID = 2;
  public static final int PROF2_ID = 3;
  public static final int ALUNO1_ID = 4;
  public static final int ALUNO2_ID = 5;

  /**
   * Roda um cenário sobre uma instância nova da aplicação. O estado vive em
   * campos estáticos, então o reset é o que impede um teste de enxergar os
   * dados do anterior.
   */
  protected void test(TestCase testCase) {
    Main.resetState();
    var config = new TestConfig();
    var app = Main.createApp();
    JavalinTest.test(app, config, testCase);
  }

  /** Cria PROF1, PROF2, ALUNO1 e ALUNO2 — nessa ordem, com ids 2 a 5. */
  public static void criar2Professores2Alunos(HttpClient client) {
    for (int i = 0; i < 2; i++) {
      var res = POST(client, "/usuarios", ADM_ID, new Body(1));
      assertEquals(201, res.code());
    }
    for (int i = 1; i < 3; i++) {
      var res = POST(client, "/usuarios", ADM_ID + i, new Body(0));
      assertEquals(201, res.code());
    }
  }

  // ---------- atalhos de cenário ----------

  /** Cria uma turma e devolve o id gerado. */
  protected static String criarTurma(HttpClient client, int profUid, String nome) {
    var res = POST(client, "/turmas", profUid, Map.of("nome", nome));
    assertEquals(201, res.code());
    return id(res);
  }

  /** Matricula um aluno na turma, pelo professor responsável. */
  protected static void matricular(HttpClient client, int profUid, String turmaId, int alunoUid) {
    var res = POST(client, "/turmas/" + turmaId + "/participantes", profUid,
        Map.of("uid", String.valueOf(alunoUid)));
    assertEquals(201, res.code());
  }

  /** Cria um post na turma e devolve o id gerado. */
  protected static String criarPost(HttpClient client, int uid, String turmaId) {
    var res = POST(client, "/turmas/" + turmaId + "/posts", uid,
        Map.of("titulo", "Aviso", "corpo", "Corpo do aviso"));
    assertEquals(201, res.code());
    return id(res);
  }

  /** Cria uma atividade na turma e devolve o id gerado. */
  protected static String criarAtividade(HttpClient client, int profUid, String turmaId) {
    var res = POST(client, "/atividades", profUid, Map.of(
        "titulo", "Trabalho", "corpo", "Enunciado",
        "dataEntrega", "2026-12-01", "turmaId", turmaId));
    assertEquals(201, res.code());
    return id(res);
  }

  /** Cria um comentário numa publicação e devolve o id gerado. */
  protected static String criarComentario(HttpClient client, int uid, String turmaId, String tipo, String pubId) {
    var res = POST(client, "/turmas/" + turmaId + "/" + tipo + "/" + pubId + "/comentarios", uid,
        Map.of("conteudo", "Comentário"));
    assertEquals(201, res.code());
    return id(res);
  }

  // ---------- leitura de respostas ----------

  private static final ObjectMapper MAPPER = new ObjectMapper();

  /** Lê o corpo da resposta como JSON. Só pode ser chamado uma vez por resposta. */
  protected static JsonNode json(Response res) {
    try {
      return MAPPER.readTree(res.body().string());
    } catch (Exception e) {
      throw new RuntimeException("Resposta não é JSON válido", e);
    }
  }

  protected static String id(Response res) {
    return json(res).get("id").asText();
  }

  // ---------- verbos HTTP ----------

  protected static Response GET(HttpClient client, String path, int UID) {
    return client.get(path, req -> {
      req.header("Cookie", "uid=" + UID);
    });
  }

  protected static Response POST(HttpClient client, String path, int UID, Object body) {
    return client.post(path, body, req -> {
      req.header("Cookie", "uid=" + UID);
    });
  }

  protected static Response PUT(HttpClient client, String path, int UID, Object body) {
    return client.put(path, body, req -> {
      req.header("Cookie", "uid=" + UID);
    });
  }

  protected static Response DELETE(HttpClient client, String path, int UID) {
    return client.delete(path, null, req -> {
      req.header("Cookie", "uid=" + UID);
    });
  }

  /** Chamada sem cookie de autenticação, para exercitar o 401. */
  protected static Response GET_ANONIMO(HttpClient client, String path) {
    return client.get(path);
  }
}
