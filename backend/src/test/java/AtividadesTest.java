import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Map;

import org.junit.jupiter.api.Test;

import io.javalin.testtools.HttpClient;
import poo.api.exceptions.ForbiddenException;
import poo.api.exceptions.NotFoundException;
import poo.api.exceptions.UnauthorizedException;

public class AtividadesTest extends ApiFixture {

  private static Map<String, String> corpoAtividade(String turmaId) {
    return Map.of("titulo", "Trabalho", "corpo", "Enunciado",
        "dataEntrega", "2026-12-01", "turmaId", turmaId);
  }

  /** PROF1 é o responsável pela turma 1 e ALUNO1 está matriculado nela. */
  private static String cenario(HttpClient client) {
    criar2Professores2Alunos(client);
    var turma = criarTurma(client, PROF1_ID, "POO");
    matricular(client, PROF1_ID, turma, ALUNO1_ID);
    return turma;
  }

  @Test
  void apenasProfessorResponsavelCria() {
    test((server, client) -> {
      var turma = cenario(client);

      assertEquals(201, POST(client, "/atividades", PROF1_ID, corpoAtividade(turma)).code());
      assertEquals(ForbiddenException.STATUS_CODE,
          POST(client, "/atividades", PROF2_ID, corpoAtividade(turma)).code());
      assertEquals(ForbiddenException.STATUS_CODE,
          POST(client, "/atividades", ALUNO1_ID, corpoAtividade(turma)).code());
      assertEquals(ForbiddenException.STATUS_CODE,
          POST(client, "/atividades", ADM_ID, corpoAtividade(turma)).code());
    });
  }

  @Test
  void criarEmTurmaInexistenteDa404() {
    test((server, client) -> {
      cenario(client);
      assertEquals(NotFoundException.STATUS_CODE,
          POST(client, "/atividades", PROF1_ID, corpoAtividade("999")).code());
    });
  }

  @Test
  void semAutenticacaoNaoAcessa() {
    test((server, client) -> {
      assertEquals(UnauthorizedException.STATUS_CODE, GET_ANONIMO(client, "/atividades").code());
    });
  }

  @Test
  void listagemMostraApenasAtividadesDasTurmasDoUsuario() {
    test((server, client) -> {
      var turma = cenario(client);
      var outraTurma = criarTurma(client, PROF2_ID, "Cálculo");
      criarAtividade(client, PROF1_ID, turma);
      criarAtividade(client, PROF2_ID, outraTurma);

      assertEquals(1, json(GET(client, "/atividades", PROF1_ID)).size());
      assertEquals(1, json(GET(client, "/atividades", ALUNO1_ID)).size());
      assertEquals(0, json(GET(client, "/atividades", ALUNO2_ID)).size());
      assertEquals(2, json(GET(client, "/atividades", ADM_ID)).size());
    });
  }

  @Test
  void filtroPorTurmaRestringeAListagem() {
    test((server, client) -> {
      var turma = cenario(client);
      var outraTurma = criarTurma(client, PROF2_ID, "Cálculo");
      criarAtividade(client, PROF1_ID, turma);
      criarAtividade(client, PROF2_ID, outraTurma);

      assertEquals(1, json(GET(client, "/atividades?turmaId=" + turma, ADM_ID)).size());
      // o filtro não contorna a permissão: PROF1 não vê a turma do PROF2
      assertEquals(0, json(GET(client, "/atividades?turmaId=" + outraTurma, PROF1_ID)).size());
    });
  }

  @Test
  void verExigeParticiparDaTurma() {
    test((server, client) -> {
      var turma = cenario(client);
      var atividade = criarAtividade(client, PROF1_ID, turma);

      assertEquals(200, GET(client, "/atividades/" + atividade, PROF1_ID).code());
      assertEquals(200, GET(client, "/atividades/" + atividade, ALUNO1_ID).code());
      assertEquals(200, GET(client, "/atividades/" + atividade, ADM_ID).code());
      assertEquals(ForbiddenException.STATUS_CODE, GET(client, "/atividades/" + atividade, PROF2_ID).code());
      assertEquals(ForbiddenException.STATUS_CODE, GET(client, "/atividades/" + atividade, ALUNO2_ID).code());
    });
  }

  @Test
  void professorResponsavelEAdminEditam() {
    test((server, client) -> {
      var turma = cenario(client);
      var atividade = criarAtividade(client, PROF1_ID, turma);
      var corpo = corpoAtividade(turma);

      assertEquals(200, PUT(client, "/atividades/" + atividade, PROF1_ID, corpo).code());
      assertEquals(200, PUT(client, "/atividades/" + atividade, ADM_ID, corpo).code());
      assertEquals(ForbiddenException.STATUS_CODE,
          PUT(client, "/atividades/" + atividade, ALUNO1_ID, corpo).code());
      assertEquals(ForbiddenException.STATUS_CODE,
          PUT(client, "/atividades/" + atividade, PROF2_ID, corpo).code());
    });
  }

  @Test
  void professorResponsavelEAdminExcluem() {
    test((server, client) -> {
      var turma = cenario(client);

      assertEquals(ForbiddenException.STATUS_CODE,
          DELETE(client, "/atividades/" + criarAtividade(client, PROF1_ID, turma), ALUNO1_ID).code());
      assertEquals(204, DELETE(client, "/atividades/" + criarAtividade(client, PROF1_ID, turma), PROF1_ID).code());
      assertEquals(204, DELETE(client, "/atividades/" + criarAtividade(client, PROF1_ID, turma), ADM_ID).code());
    });
  }

  @Test
  void atividadeInexistenteDa404() {
    test((server, client) -> {
      cenario(client);
      assertEquals(NotFoundException.STATUS_CODE, GET(client, "/atividades/999", PROF1_ID).code());
    });
  }

  @Test
  void excluirAtividadeRemoveSeusComentarios() {
    test((server, client) -> {
      var turma = cenario(client);
      var atividade = criarAtividade(client, PROF1_ID, turma);
      var comentario = criarComentario(client, ALUNO1_ID, turma, "atividades", atividade);

      assertEquals(204, DELETE(client, "/atividades/" + atividade, PROF1_ID).code());
      assertEquals(NotFoundException.STATUS_CODE,
          GET(client, "/turmas/" + turma + "/atividades/" + atividade + "/comentarios/" + comentario,
              PROF1_ID).code());
    });
  }
}
