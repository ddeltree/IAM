import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Map;

import org.junit.jupiter.api.Test;

import poo.api.exceptions.ForbiddenException;
import poo.api.exceptions.NotFoundException;
import poo.api.exceptions.UnauthorizedException;

public class TurmasTest extends ApiFixture {

  @Test
  void apenasProfessorCriaTurma() {
    test((server, client) -> {
      criar2Professores2Alunos(client);

      assertEquals(201, POST(client, "/turmas", PROF1_ID, Map.of("nome", "POO")).code());
      // aluno e admin não têm CRIAR_TURMA
      assertEquals(ForbiddenException.STATUS_CODE,
          POST(client, "/turmas", ALUNO1_ID, Map.of("nome", "POO")).code());
      assertEquals(ForbiddenException.STATUS_CODE,
          POST(client, "/turmas", ADM_ID, Map.of("nome", "POO")).code());
    });
  }

  @Test
  void semAutenticacaoNaoAcessa() {
    test((server, client) -> {
      assertEquals(UnauthorizedException.STATUS_CODE, GET_ANONIMO(client, "/turmas").code());
    });
  }

  @Test
  void listagemMostraSomenteAsTurmasDoUsuario() {
    test((server, client) -> {
      criar2Professores2Alunos(client);
      var turmaDoProf1 = criarTurma(client, PROF1_ID, "POO");
      criarTurma(client, PROF2_ID, "Cálculo");
      matricular(client, PROF1_ID, turmaDoProf1, ALUNO1_ID);

      // cada professor enxerga apenas a turma que criou
      assertEquals(1, json(GET(client, "/turmas", PROF1_ID)).size());
      assertEquals(1, json(GET(client, "/turmas", PROF2_ID)).size());
      // o aluno matriculado enxerga a turma dele; o outro, nenhuma
      assertEquals(1, json(GET(client, "/turmas", ALUNO1_ID)).size());
      assertEquals(0, json(GET(client, "/turmas", ALUNO2_ID)).size());
      // o admin enxerga todas
      assertEquals(2, json(GET(client, "/turmas", ADM_ID)).size());
    });
  }

  @Test
  void verTurmaExigeSerParticipanteOuAdmin() {
    test((server, client) -> {
      criar2Professores2Alunos(client);
      var turma = criarTurma(client, PROF1_ID, "POO");
      matricular(client, PROF1_ID, turma, ALUNO1_ID);

      assertEquals(200, GET(client, "/turmas/" + turma, PROF1_ID).code());
      assertEquals(200, GET(client, "/turmas/" + turma, ALUNO1_ID).code());
      assertEquals(200, GET(client, "/turmas/" + turma, ADM_ID).code());
      // de fora da turma, ninguém vê
      assertEquals(ForbiddenException.STATUS_CODE, GET(client, "/turmas/" + turma, PROF2_ID).code());
      assertEquals(ForbiddenException.STATUS_CODE, GET(client, "/turmas/" + turma, ALUNO2_ID).code());
    });
  }

  @Test
  void turmaInexistenteDa404() {
    test((server, client) -> {
      criar2Professores2Alunos(client);
      assertEquals(NotFoundException.STATUS_CODE, GET(client, "/turmas/999", PROF1_ID).code());
    });
  }

  @Test
  void apenasProfessorResponsavelEditaEExclui() {
    test((server, client) -> {
      criar2Professores2Alunos(client);
      var turma = criarTurma(client, PROF1_ID, "POO");
      matricular(client, PROF1_ID, turma, ALUNO1_ID);

      assertEquals(ForbiddenException.STATUS_CODE,
          PUT(client, "/turmas/" + turma, PROF2_ID, Map.of("nome", "Roubada")).code());
      assertEquals(ForbiddenException.STATUS_CODE,
          PUT(client, "/turmas/" + turma, ALUNO1_ID, Map.of("nome", "Roubada")).code());
      assertEquals(ForbiddenException.STATUS_CODE, DELETE(client, "/turmas/" + turma, PROF2_ID).code());
      assertEquals(ForbiddenException.STATUS_CODE, DELETE(client, "/turmas/" + turma, ALUNO1_ID).code());

      assertEquals(200, PUT(client, "/turmas/" + turma, PROF1_ID, Map.of("nome", "POO II")).code());
      assertEquals(204, DELETE(client, "/turmas/" + turma, PROF1_ID).code());
      assertEquals(NotFoundException.STATUS_CODE, GET(client, "/turmas/" + turma, PROF1_ID).code());
    });
  }

  @Test
  void excluirTurmaRemovePostsAtividadesEComentarios() {
    test((server, client) -> {
      criar2Professores2Alunos(client);
      var turma = criarTurma(client, PROF1_ID, "POO");
      matricular(client, PROF1_ID, turma, ALUNO1_ID);
      var post = criarPost(client, PROF1_ID, turma);
      var atividade = criarAtividade(client, PROF1_ID, turma);
      criarComentario(client, ALUNO1_ID, turma, "posts", post);
      criarComentario(client, ALUNO1_ID, turma, "atividades", atividade);

      assertEquals(204, DELETE(client, "/turmas/" + turma, PROF1_ID).code());

      // nada da turma sobrevive
      assertEquals(NotFoundException.STATUS_CODE, GET(client, "/turmas/" + turma + "/posts", PROF1_ID).code());
      assertEquals(NotFoundException.STATUS_CODE, GET(client, "/atividades/" + atividade, PROF1_ID).code());
      assertTrue(json(GET(client, "/atividades", PROF1_ID)).isEmpty());
    });
  }
}
