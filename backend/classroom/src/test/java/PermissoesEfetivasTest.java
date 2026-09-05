import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/**
 * O endpoint que responde "o que eu posso fazer aqui?".
 *
 * É o que permite a interface esconder um botão sem reescrever as regras do
 * lado dela — e, de quebra, torna o modelo de permissões observável de fora.
 */
public class PermissoesEfetivasTest extends ApiFixture {

  @Test
  void oQueCadaPapelPodeCriar() {
    test((server, client) -> {
      criar2Professores2Alunos(client);

      var admin = json(GET(client, "/permissoes", ADM_ID)).get("global");
      assertFalse(admin.get("CRIAR_TURMA").asBoolean(), "o admin não cria turmas");
      assertTrue(admin.get("CRIAR_PROFESSOR").asBoolean());
      assertFalse(admin.get("CRIAR_ALUNO").asBoolean());
      assertTrue(admin.get("LISTAR_USUARIOS").asBoolean());

      var prof = json(GET(client, "/permissoes", PROF1_ID)).get("global");
      assertTrue(prof.get("CRIAR_TURMA").asBoolean());
      assertTrue(prof.get("CRIAR_ALUNO").asBoolean());
      assertFalse(prof.get("CRIAR_PROFESSOR").asBoolean(), "só o admin cria professores");
      assertFalse(prof.get("LISTAR_USUARIOS").asBoolean());

      var aluno = json(GET(client, "/permissoes", ALUNO1_ID)).get("global");
      assertFalse(aluno.get("CRIAR_TURMA").asBoolean());
      assertFalse(aluno.get("CRIAR_ALUNO").asBoolean());
    });
  }

  @Test
  void oPrincipalVemJuntoComOPapel() {
    test((server, client) -> {
      criar2Professores2Alunos(client);

      var admin = json(GET(client, "/permissoes", ADM_ID)).get("principal");
      assertEquals("ADMIN", admin.get("papel").asText());
      assertEquals("1", admin.get("id").asText());

      assertEquals("PROFESSOR",
          json(GET(client, "/permissoes", PROF1_ID)).get("principal").get("papel").asText());
      assertEquals("ALUNO",
          json(GET(client, "/permissoes", ALUNO1_ID)).get("principal").get("papel").asText());
    });
  }

  /**
   * A assimetria central do modelo, agora visível pela API: o professor
   * responsável apaga qualquer post da turma, mas só edita os próprios.
   */
  @Test
  void noPostDoAlunoOProfessorExcluiMasNaoEdita() {
    test((server, client) -> {
      criar2Professores2Alunos(client);
      var turmaId = criarTurma(client, PROF1_ID, "POO");
      matricular(client, PROF1_ID, turmaId, ALUNO1_ID);
      var postId = criarPost(client, ALUNO1_ID, turmaId);

      var prof = permissoesDe(client, PROF1_ID, "POST/" + postId);
      assertTrue(prof.get("EXCLUIR_POST").asBoolean(), "moderação da turma");
      assertFalse(prof.get("EDITAR_POST").asBoolean(), "editar é do autor");

      var autor = permissoesDe(client, ALUNO1_ID, "POST/" + postId);
      assertTrue(autor.get("EDITAR_POST").asBoolean());
      assertTrue(autor.get("EXCLUIR_POST").asBoolean());

      // o admin modera tudo, mas não é autor de nada
      var admin = permissoesDe(client, ADM_ID, "POST/" + postId);
      assertTrue(admin.get("EDITAR_POST").asBoolean());
      assertTrue(admin.get("EXCLUIR_POST").asBoolean());
    });
  }

  @Test
  void permissoesDeUmaTurmaNaoVazamParaOutroTipo() {
    test((server, client) -> {
      criar2Professores2Alunos(client);
      var turmaId = criarTurma(client, PROF1_ID, "POO");

      var mapa = permissoesDe(client, ADM_ID, "TURMA/" + turmaId);
      assertTrue(mapa.has("VER_TURMA"));
      // o admin modera, não produz: nem post nem comentário partem dele
      assertFalse(mapa.get("CRIAR_POST").asBoolean());
      assertFalse(mapa.get("CRIAR_COMENTARIO").asBoolean());
      assertFalse(mapa.get("CRIAR_ATIVIDADE").asBoolean());
      // EDITAR_POST age sobre POST; não faz sentido perguntar sobre uma turma
      assertFalse(mapa.has("EDITAR_POST"));
      // nem as ações sem alvo entram no mapa de um recurso
      assertFalse(mapa.has("CRIAR_TURMA"));
    });
  }

  @Test
  void professorDeForaNaoEnxergaNadaDaTurmaAlheia() {
    test((server, client) -> {
      criar2Professores2Alunos(client);
      var turmaId = criarTurma(client, PROF1_ID, "POO");

      var deFora = permissoesDe(client, PROF2_ID, "TURMA/" + turmaId);
      assertFalse(deFora.get("VER_TURMA").asBoolean());
      assertFalse(deFora.get("EDITAR_TURMA").asBoolean());
      assertFalse(deFora.get("CRIAR_POST").asBoolean());
      assertFalse(deFora.get("MATRICULAR_ALUNO").asBoolean());
    });
  }

  @Test
  void referenciaDesconhecidaViraMapaVazio() {
    test((server, client) -> {
      criar2Professores2Alunos(client);
      // fecha do lado seguro: sem permissão nenhuma, em vez de erro
      assertEquals(0, permissoesDe(client, PROF1_ID, "TURMA/999").size());
      assertEquals(0, permissoesDe(client, PROF1_ID, "COISA/1").size());
    });
  }

  @Test
  void explicarDizQualClausulaDecidiu() {
    test((server, client) -> {
      criar2Professores2Alunos(client);
      var turmaId = criarTurma(client, PROF1_ID, "POO");

      var res = GET(client, "/permissoes?recurso=TURMA/" + turmaId + "&explicar=true", PROF1_ID);
      var editar = json(res).get("recursos").get("TURMA/" + turmaId).get("EDITAR_TURMA");

      assertTrue(editar.get("permitido").asBoolean());
      assertEquals("ALLOW:EDITAR_TURMA:TURMA", editar.get("sid").asText());
      assertEquals("Professores", editar.get("origem").asText());
    });
  }

  @Test
  void anonimoNaoConsultaPermissoes() {
    test((server, client) -> {
      assertEquals(401, GET_ANONIMO(client, "/permissoes").code());
    });
  }

  private static com.fasterxml.jackson.databind.JsonNode permissoesDe(
      io.javalin.testtools.HttpClient client, int uid, String ref) {
    var res = GET(client, "/permissoes?recurso=" + ref, uid);
    assertEquals(200, res.code());
    return json(res).get("recursos").get(ref);
  }
}
