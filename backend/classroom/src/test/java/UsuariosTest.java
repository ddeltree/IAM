import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

import okhttp3.Response;
import poo.api.exceptions.ForbiddenException;

public class UsuariosTest extends ApiFixture {
  @Test
  void listarUsuarios() {
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

}
