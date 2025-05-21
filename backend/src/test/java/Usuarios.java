import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

import io.javalin.testtools.HttpClient;
import io.javalin.testtools.JavalinTest;
import io.javalin.testtools.TestCase;
import okhttp3.Response;
import poo.Main;
import poo.api.exceptions.ForbiddenException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Usuarios {

  private static final Logger log = LoggerFactory.getLogger(Usuarios.class);

  public static class Body {
    public Body() {
    }

    public Body(int tipo) {
      this.tipo = tipo;
    }

    public String name = "Nome de usuário";
    public int tipo = 1;
  }

  @Test
  void listarUsuarios() {
    int ADM_ID = 1;
    // Usuário ADMIN
    test((server, client) -> {
      var res = GET(client, "/usuarios", ADM_ID);
      assertEquals(200, res.code());
    });

    // Usuário inexistente
    test((server, client) -> {
      var res = GET(client, "/usuarios", 2);
      assertEquals(404, res.code());
    });

    // professor e aluno não podem listar
    test((server, client) -> {
      Response res;
      var body = new Body();
      // criar professor
      res = POST(client, "/usuarios", ADM_ID, body);
      assertEquals(201, res.code());
      int idProfessor = 2;
      // professor pede listagem
      res = GET(client, "/usuarios", idProfessor);
      assertEquals(ForbiddenException.STATUS_CODE, res.code());
      // criar aluno
      body.tipo = 0;
      res = POST(client, "/usuarios", idProfessor, body);
      assertEquals(201, res.code());
      int idAluno = 3;
      // aluno pede listagem
      res = GET(client, "/usuarios", idAluno);
      assertEquals(ForbiddenException.STATUS_CODE, res.code());
    });
  }

  @Test
  void verPerfil() {
    // usuários podem ver seus próprios perfis
    test((server, client) -> {
      criar2Professores2Alunos(client);
      for (int i = 2; i < 6; i++) {
        var res = GET(client, "/usuarios/" + i, i);
        assertEquals(200, res.code());
      }
    });
    // outros usuários não podem ver perfis de outros que não os seus
    test((server, client) -> {
      criar2Professores2Alunos(client);
      for (int uid = 2; uid < 6; uid++) {
        for (int id = 2; id < 6; id++) {
          if (uid == id)
            continue;
          var res = GET(client, "/usuarios/" + id, uid);
          assertEquals(ForbiddenException.STATUS_CODE, res.code());
        }
      }
    });
  }

  public static final int ADM_ID = 1;
  public static final int PROF1_ID = 2;
  public static final int PROF2_ID = 3;
  public static final int ALUNO1_ID = 4;
  public static final int ALUNO2_ID = 5;

  public static void criar2Professores2Alunos(HttpClient client) {
    for (int i = 0; i < 2; i++) {
      var res = POST(client, "/usuarios", ADM_ID, new Body(1));
      assertEquals(201, res.code());
    }
    for (int i = 1; i < 3; i++) {
      int _i = i;
      var res = POST(client, "/usuarios", ADM_ID + _i, new Body(0));
      assertEquals(201, res.code());
    }
  }

  private static void test(TestCase testCase) {
    JavalinTest.test(Main.createApp(), testCase);
  }

  private static Response GET(HttpClient client, String path, int UID) {
    return client.get(path, req -> {
      req.header("Cookie", "uid=" + UID);
    });
  }

  private static Response POST(HttpClient client, String path, int UID, Object body) {
    return client.post(path, body, req -> {
      req.header("Cookie", "uid=" + UID);
    });
  }

  private static Response PUT(HttpClient client, String path, int UID, Object body) {
    return client.put(path, body, req -> {
      req.header("Cookie", "uid=" + UID);
    });
  }

  private static Response DELETE(HttpClient client, String path, int UID) {
    return client.delete(path, null, req -> {
      req.header("Cookie", "uid=" + UID);
    });
  }

}
