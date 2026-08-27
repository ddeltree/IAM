import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Map;

import org.junit.jupiter.api.Test;

import io.javalin.testtools.HttpClient;
import poo.api.exceptions.ForbiddenException;
import poo.api.exceptions.NotFoundException;

public class ComentariosTest extends ApiFixture {

  private static final Map<String, String> CORPO = Map.of("conteudo", "Comentário");

  /** PROF1 é o responsável pela turma 1, com ALUNO1 e ALUNO2 matriculados. */
  private static String cenario(HttpClient client) {
    criar2Professores2Alunos(client);
    var turma = criarTurma(client, PROF1_ID, "POO");
    matricular(client, PROF1_ID, turma, ALUNO1_ID);
    matricular(client, PROF1_ID, turma, ALUNO2_ID);
    return turma;
  }

  private static String comentariosDe(String turma, String tipo, String pubId) {
    return "/turmas/" + turma + "/" + tipo + "/" + pubId + "/comentarios";
  }

  @Test
  void participantesComentamEmPostsEAtividades() {
    test((server, client) -> {
      var turma = cenario(client);
      var post = criarPost(client, PROF1_ID, turma);
      var atividade = criarAtividade(client, PROF1_ID, turma);

      assertEquals(201, POST(client, comentariosDe(turma, "posts", post), ALUNO1_ID, CORPO).code());
      assertEquals(201, POST(client, comentariosDe(turma, "atividades", atividade), ALUNO1_ID, CORPO).code());
      assertEquals(201, POST(client, comentariosDe(turma, "posts", post), PROF1_ID, CORPO).code());
    });
  }

  @Test
  void quemNaoEDaTurmaNaoComenta() {
    test((server, client) -> {
      var turma = cenario(client);
      var post = criarPost(client, PROF1_ID, turma);

      assertEquals(ForbiddenException.STATUS_CODE,
          POST(client, comentariosDe(turma, "posts", post), PROF2_ID, CORPO).code());
    });
  }

  @Test
  void comentarioApareceNaListagemDaPublicacao() {
    test((server, client) -> {
      var turma = cenario(client);
      var post = criarPost(client, PROF1_ID, turma);
      criarComentario(client, ALUNO1_ID, turma, "posts", post);
      criarComentario(client, ALUNO2_ID, turma, "posts", post);

      assertEquals(2, json(GET(client, comentariosDe(turma, "posts", post), PROF1_ID)).size());
    });
  }

  @Test
  void listarExigeParticiparDaTurma() {
    test((server, client) -> {
      var turma = cenario(client);
      var post = criarPost(client, PROF1_ID, turma);

      assertEquals(200, GET(client, comentariosDe(turma, "posts", post), ADM_ID).code());
      assertEquals(ForbiddenException.STATUS_CODE,
          GET(client, comentariosDe(turma, "posts", post), PROF2_ID).code());
    });
  }

  @Test
  void apenasAutorOuAdminEdita() {
    test((server, client) -> {
      var turma = cenario(client);
      var post = criarPost(client, PROF1_ID, turma);
      var comentario = criarComentario(client, ALUNO1_ID, turma, "posts", post);
      var caminho = comentariosDe(turma, "posts", post) + "/" + comentario;

      assertEquals(200, PUT(client, caminho, ALUNO1_ID, CORPO).code());
      assertEquals(200, PUT(client, caminho, ADM_ID, CORPO).code());
      assertEquals(ForbiddenException.STATUS_CODE, PUT(client, caminho, ALUNO2_ID, CORPO).code());
      // nem o professor responsável edita comentário alheio
      assertEquals(ForbiddenException.STATUS_CODE, PUT(client, caminho, PROF1_ID, CORPO).code());
    });
  }

  @Test
  void autorProfessorEAdminExcluem() {
    test((server, client) -> {
      var turma = cenario(client);
      var post = criarPost(client, PROF1_ID, turma);
      var base = comentariosDe(turma, "posts", post) + "/";

      assertEquals(ForbiddenException.STATUS_CODE,
          DELETE(client, base + criarComentario(client, ALUNO1_ID, turma, "posts", post), ALUNO2_ID).code());
      assertEquals(204,
          DELETE(client, base + criarComentario(client, ALUNO1_ID, turma, "posts", post), ALUNO1_ID).code());
      assertEquals(204,
          DELETE(client, base + criarComentario(client, ALUNO1_ID, turma, "posts", post), PROF1_ID).code());
      assertEquals(204,
          DELETE(client, base + criarComentario(client, ALUNO1_ID, turma, "posts", post), ADM_ID).code());
    });
  }

  @Test
  void comentarioSaiDaListagemAoSerExcluido() {
    test((server, client) -> {
      var turma = cenario(client);
      var post = criarPost(client, PROF1_ID, turma);
      var comentario = criarComentario(client, ALUNO1_ID, turma, "posts", post);

      assertEquals(204, DELETE(client, comentariosDe(turma, "posts", post) + "/" + comentario, ALUNO1_ID).code());
      assertEquals(0, json(GET(client, comentariosDe(turma, "posts", post), PROF1_ID)).size());
    });
  }

  @Test
  void comentarioDeOutraPublicacaoNaoEAcessivelPelaUrl() {
    test((server, client) -> {
      var turma = cenario(client);
      var post = criarPost(client, PROF1_ID, turma);
      var outroPost = criarPost(client, PROF1_ID, turma);
      var comentario = criarComentario(client, ALUNO1_ID, turma, "posts", post);

      assertEquals(NotFoundException.STATUS_CODE,
          GET(client, comentariosDe(turma, "posts", outroPost) + "/" + comentario, PROF1_ID).code());
    });
  }

  @Test
  void tipoDePublicacaoInvalidoDa404() {
    test((server, client) -> {
      var turma = cenario(client);
      var post = criarPost(client, PROF1_ID, turma);

      assertEquals(NotFoundException.STATUS_CODE,
          GET(client, comentariosDe(turma, "avisos", post), PROF1_ID).code());
    });
  }
}
