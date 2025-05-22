import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

import io.javalin.testtools.HttpClient;
import io.javalin.testtools.JavalinTest;
import io.javalin.testtools.TestCase;
import io.javalin.testtools.TestConfig;
import okhttp3.Response;
import poo.Main;
import poo.api.exceptions.ForbiddenException;

public class Usuarios {
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

  @Test
  void adminPodeCriarProfessorEProfessorCriaAluno() {
    // admin pode criar professores, professores podem criar alunos
    test((server, client) -> {
      criar2Professores2Alunos(client);
    });
  }

  @Test
  void adminNaoCriaAlunos() {
    // admin não pode criar alunos
    test((server, client) -> {
      var res = POST(client, "/usuarios", ADM_ID, new Body(0));
      assertEquals(ForbiddenException.STATUS_CODE, res.code());
    });
  }

  @Test
  void professorNaoCriaProfessores() {
    // professor não pode criar professores
    test((server, client) -> {
      criar2Professores2Alunos(client);
      var res = POST(client, "/usuarios", PROF1_ID, new Body(1));
      assertEquals(ForbiddenException.STATUS_CODE, res.code());
    });
  }

  @Test
  void ninguemCriaTipoInvalido() {
    // ninguém pode criar um tipo inválido de usuário
    test((server, client) -> {
      criar2Professores2Alunos(client);
      var status = 400;
      var tiposInvalidos = new int[] { -1, 2 };
      for (int tipo : tiposInvalidos) {
        var res = POST(client, "/usuarios", ADM_ID, new Body(tipo));
        assertEquals(status, res.code());

        res = POST(client, "/usuarios", PROF1_ID, new Body(tipo));
        assertEquals(status, res.code());

        res = POST(client, "/usuarios", ALUNO1_ID, new Body(tipo));
        assertEquals(status, res.code());
      }
    });
  }

  @Test
  void usuarioPodeAtualizarSeuPerfil() {
    // apenas o próprio usuário pode atualizar o seu perfil
    test((server, client) -> {
      criar2Professores2Alunos(client);
      for (int i = 2; i < 6; i++) {
        var res = PUT(client, "/usuarios/" + i, i, new Body());
        assertEquals(200, res.code());
      }
    });
  }

  @Test
  void outrosUsuariosNaoPodeAtualizarPerfil() {
    // outros usuários não podem alterar o perfil de outros
    test((server, client) -> {
      criar2Professores2Alunos(client);
      for (int uid = 2; uid < 6; uid++) {
        for (int targetId = 2; targetId < 6; targetId++) {
          if (uid == targetId)
            continue;
          var res = PUT(client, "/usuarios/" + targetId, uid, new Body());
          assertEquals(ForbiddenException.STATUS_CODE, res.code());
        }
      }
    });
  }

  @Test
  void usuarioPodeSeDeletar() {
    // outros usuários podem deletar seus próprios perfis
    test((server, client) -> {
      criar2Professores2Alunos(client);
      for (int i = 2; i < 6; i++) {
        var res = DELETE(client, "/usuarios/" + i, i);
        assertEquals(204, res.code());
      }
    });
  }

  @Test
  void outrosUsuariosNaoPodeDeletar() {
    // outros usuários não podem deletar perfis de outros
    test((server, client) -> {
      criar2Professores2Alunos(client);
      for (int uid = 2; uid < 6; uid++) {
        for (int targetId = 2; targetId < 6; targetId++) {
          if (uid == targetId)
            continue;
          var res = DELETE(client, "/usuarios/" + targetId, uid);
          assertEquals(ForbiddenException.STATUS_CODE, res.code());
        }
      }
    });
  }

  @Test
  void adminPodeDeletarUsuarios() {
    // admin pode deletar outros perfis
    test((server, client) -> {
      criar2Professores2Alunos(client);
      for (int i = 2; i < 6; i++) {
        var res = DELETE(client, "/usuarios/" + i, ADM_ID);
        assertEquals(204, res.code());
      }
    });
  }

  @Test
  void ninguemPodeDeletarAdmin() {
    // usuários comuns não podem deletar o ADMIN
    test((server, client) -> {
      criar2Professores2Alunos(client);
      for (int i = 2; i < 6; i++) {
        var res = DELETE(client, "/usuarios/" + ADM_ID, i);
        assertEquals(ForbiddenException.STATUS_CODE, res.code());
      }
    });
  }

  private void test(TestCase testCase) {
    var config = new TestConfig();

    var app = Main.createApp();
    JavalinTest.test(app, config, testCase);
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
