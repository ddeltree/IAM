import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Map;

import org.junit.jupiter.api.Test;

import io.javalin.testtools.HttpClient;
import poo.api.exceptions.ForbiddenException;
import poo.api.exceptions.NotFoundException;

public class ParticipantesTest extends ApiFixture {

  private static String cenario(HttpClient client) {
    criar2Professores2Alunos(client);
    return criarTurma(client, PROF1_ID, "POO");
  }

  private static Map<String, String> aluno(int uid) {
    return Map.of("uid", String.valueOf(uid));
  }

  @Test
  void apenasProfessorResponsavelMatricula() {
    test((server, client) -> {
      var turma = cenario(client);
      var caminho = "/turmas/" + turma + "/participantes";

      // um professor de outra turma não pode matricular na turma alheia
      assertEquals(ForbiddenException.STATUS_CODE, POST(client, caminho, PROF2_ID, aluno(ALUNO1_ID)).code());
      assertEquals(ForbiddenException.STATUS_CODE, POST(client, caminho, ALUNO1_ID, aluno(ALUNO2_ID)).code());
      assertEquals(201, POST(client, caminho, PROF1_ID, aluno(ALUNO1_ID)).code());
    });
  }

  @Test
  void soAlunoPodeSerMatriculado() {
    test((server, client) -> {
      var turma = cenario(client);

      assertEquals(400,
          POST(client, "/turmas/" + turma + "/participantes", PROF1_ID, aluno(PROF2_ID)).code());
    });
  }

  @Test
  void matricularUsuarioInexistenteDa404() {
    test((server, client) -> {
      var turma = cenario(client);

      assertEquals(NotFoundException.STATUS_CODE,
          POST(client, "/turmas/" + turma + "/participantes", PROF1_ID, aluno(999)).code());
    });
  }

  @Test
  void listagemIncluiOProfessorResponsavel() {
    test((server, client) -> {
      var turma = cenario(client);
      matricular(client, PROF1_ID, turma, ALUNO1_ID);

      var participantes = json(GET(client, "/turmas/" + turma + "/participantes", PROF1_ID));
      assertEquals(2, participantes.size());
      assertTrue(participantes.findValuesAsText("userId").contains(String.valueOf(PROF1_ID)));
      assertTrue(participantes.findValuesAsText("userId").contains(String.valueOf(ALUNO1_ID)));
    });
  }

  @Test
  void listagemExigeParticiparDaTurma() {
    test((server, client) -> {
      var turma = cenario(client);
      matricular(client, PROF1_ID, turma, ALUNO1_ID);
      var caminho = "/turmas/" + turma + "/participantes";

      assertEquals(200, GET(client, caminho, ALUNO1_ID).code());
      assertEquals(200, GET(client, caminho, ADM_ID).code());
      assertEquals(ForbiddenException.STATUS_CODE, GET(client, caminho, PROF2_ID).code());
      assertEquals(ForbiddenException.STATUS_CODE, GET(client, caminho, ALUNO2_ID).code());
    });
  }

  @Test
  void apenasProfessorResponsavelDesmatricula() {
    test((server, client) -> {
      var turma = cenario(client);
      matricular(client, PROF1_ID, turma, ALUNO1_ID);
      var caminho = "/turmas/" + turma + "/participantes/" + ALUNO1_ID;

      assertEquals(ForbiddenException.STATUS_CODE, DELETE(client, caminho, PROF2_ID).code());
      assertEquals(ForbiddenException.STATUS_CODE, DELETE(client, caminho, ALUNO1_ID).code());
      assertEquals(204, DELETE(client, caminho, PROF1_ID).code());
      assertEquals(1, json(GET(client, "/turmas/" + turma + "/participantes", PROF1_ID)).size());
    });
  }

  @Test
  void desmatricularQuemNaoEstaNaTurmaDa404() {
    test((server, client) -> {
      var turma = cenario(client);

      assertEquals(NotFoundException.STATUS_CODE,
          DELETE(client, "/turmas/" + turma + "/participantes/" + ALUNO1_ID, PROF1_ID).code());
    });
  }

  @Test
  void professorNaoRemoveASiMesmo() {
    test((server, client) -> {
      var turma = cenario(client);

      assertEquals(400, DELETE(client, "/turmas/" + turma + "/participantes/" + PROF1_ID, PROF1_ID).code());
    });
  }

  @Test
  void desmatricularDevolveOAcessoAoEstadoAnterior() {
    test((server, client) -> {
      var turma = cenario(client);
      matricular(client, PROF1_ID, turma, ALUNO1_ID);
      assertEquals(200, GET(client, "/turmas/" + turma, ALUNO1_ID).code());

      assertEquals(204, DELETE(client, "/turmas/" + turma + "/participantes/" + ALUNO1_ID, PROF1_ID).code());
      assertEquals(ForbiddenException.STATUS_CODE, GET(client, "/turmas/" + turma, ALUNO1_ID).code());
    });
  }
}
