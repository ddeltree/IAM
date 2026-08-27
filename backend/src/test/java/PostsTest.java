import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Map;

import org.junit.jupiter.api.Test;

import io.javalin.testtools.HttpClient;
import poo.api.exceptions.ForbiddenException;
import poo.api.exceptions.NotFoundException;

public class PostsTest extends ApiFixture {

  /** PROF1 é o responsável pela turma 1 e ALUNO1 está matriculado nela. */
  private static String cenario(HttpClient client) {
    criar2Professores2Alunos(client);
    var turma = criarTurma(client, PROF1_ID, "POO");
    matricular(client, PROF1_ID, turma, ALUNO1_ID);
    return turma;
  }

  @Test
  void participantesPostam() {
    test((server, client) -> {
      var turma = cenario(client);
      var corpo = Map.of("titulo", "Aviso", "corpo", "Prova na sexta");

      assertEquals(201, POST(client, "/turmas/" + turma + "/posts", PROF1_ID, corpo).code());
      assertEquals(201, POST(client, "/turmas/" + turma + "/posts", ALUNO1_ID, corpo).code());
    });
  }

  @Test
  void quemNaoEDaTurmaNaoPosta() {
    test((server, client) -> {
      var turma = cenario(client);
      var corpo = Map.of("titulo", "Aviso", "corpo", "Prova na sexta");

      assertEquals(ForbiddenException.STATUS_CODE,
          POST(client, "/turmas/" + turma + "/posts", PROF2_ID, corpo).code());
      assertEquals(ForbiddenException.STATUS_CODE,
          POST(client, "/turmas/" + turma + "/posts", ALUNO2_ID, corpo).code());
      // o admin também não posta: participar da turma é requisito de CRIAR_POST
      assertEquals(ForbiddenException.STATUS_CODE,
          POST(client, "/turmas/" + turma + "/posts", ADM_ID, corpo).code());
    });
  }

  @Test
  void listarEVerExigemParticiparDaTurma() {
    test((server, client) -> {
      var turma = cenario(client);
      var post = criarPost(client, PROF1_ID, turma);

      assertEquals(200, GET(client, "/turmas/" + turma + "/posts", ALUNO1_ID).code());
      assertEquals(200, GET(client, "/turmas/" + turma + "/posts/" + post, ADM_ID).code());
      assertEquals(ForbiddenException.STATUS_CODE,
          GET(client, "/turmas/" + turma + "/posts", ALUNO2_ID).code());
      assertEquals(ForbiddenException.STATUS_CODE,
          GET(client, "/turmas/" + turma + "/posts/" + post, PROF2_ID).code());
    });
  }

  @Test
  void apenasAutorOuAdminEdita() {
    test((server, client) -> {
      var turma = cenario(client);
      var post = criarPost(client, ALUNO1_ID, turma);
      var corpo = Map.of("titulo", "Editado", "corpo", "Novo corpo");
      var caminho = "/turmas/" + turma + "/posts/" + post;

      assertEquals(200, PUT(client, caminho, ALUNO1_ID, corpo).code());
      assertEquals(200, PUT(client, caminho, ADM_ID, corpo).code());
      // nem o professor responsável edita post alheio
      assertEquals(ForbiddenException.STATUS_CODE, PUT(client, caminho, PROF1_ID, corpo).code());
    });
  }

  @Test
  void autorProfessorEAdminExcluem() {
    test((server, client) -> {
      var turma = cenario(client);
      var caminho = "/turmas/" + turma + "/posts/";

      // o professor responsável pela turma pode apagar post de aluno
      assertEquals(204, DELETE(client, caminho + criarPost(client, ALUNO1_ID, turma), PROF1_ID).code());
      // o autor pode apagar o próprio
      assertEquals(204, DELETE(client, caminho + criarPost(client, ALUNO1_ID, turma), ALUNO1_ID).code());
      // o admin também
      assertEquals(204, DELETE(client, caminho + criarPost(client, ALUNO1_ID, turma), ADM_ID).code());
    });
  }

  @Test
  void alunoNaoApagaPostDeOutro() {
    test((server, client) -> {
      var turma = cenario(client);
      matricular(client, PROF1_ID, turma, ALUNO2_ID);
      var post = criarPost(client, ALUNO1_ID, turma);

      assertEquals(ForbiddenException.STATUS_CODE,
          DELETE(client, "/turmas/" + turma + "/posts/" + post, ALUNO2_ID).code());
    });
  }

  @Test
  void postDeOutraTurmaNaoEAcessivelPelaUrl() {
    test((server, client) -> {
      var turma = cenario(client);
      var outraTurma = criarTurma(client, PROF2_ID, "Cálculo");
      var post = criarPost(client, PROF1_ID, turma);

      // o id existe, mas não pertence à turma do caminho
      assertEquals(NotFoundException.STATUS_CODE,
          GET(client, "/turmas/" + outraTurma + "/posts/" + post, PROF2_ID).code());
    });
  }

  @Test
  void excluirPostRemoveSeusComentarios() {
    test((server, client) -> {
      var turma = cenario(client);
      var post = criarPost(client, PROF1_ID, turma);
      var comentario = criarComentario(client, ALUNO1_ID, turma, "posts", post);

      assertEquals(204, DELETE(client, "/turmas/" + turma + "/posts/" + post, PROF1_ID).code());
      assertEquals(NotFoundException.STATUS_CODE,
          GET(client, "/turmas/" + turma + "/posts/" + post + "/comentarios/" + comentario, PROF1_ID).code());
    });
  }
}
