import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.ObjectMapper;

import poo.classroom.Comentario;
import poo.classroom.Post;
import poo.classroom.Turma;
import poo.classroom.iam.SecurityContext;
import poo.iam.ContextResolver;
import poo.iam.MembershipManager;
import poo.iam.PolicyJson;
import poo.iam.RequestContext;
import poo.iam.Resource;
import poo.iam.Statement;
import poo.iam.User;
import poo.iam.condition.ConditionJson;

/**
 * A política como documento.
 *
 * O teste que importa aqui é o de equivalência: escrever cada condição em JSON,
 * ler de volta e conferir que ela decide igual em todos os cenários. Se a ida e
 * volta preserva a decisão, a política é mesmo dado — e não uma descrição
 * aproximada do que o código faz.
 */
public class PoliticaJsonTest extends ApiFixture {

  private static final ObjectMapper MAPPER = new ObjectMapper();

  @Test
  void aPoliticaSeEscreveComoDocumento() throws Exception {
    var auth = SecurityContext.getInstance();
    auth.reset();

    var json = MAPPER.valueToTree(PolicyJson.deGrupo(auth.getProfessores())).toString();

    assertTrue(json.contains("\"principal\":\"Professores\""));
    assertTrue(json.contains("ALLOW:EXCLUIR_POST:POST"));
    // a condição aparece legível, com a variável de política preservada
    assertTrue(json.contains("turma:professorId"));
    assertTrue(json.contains("${principal:id}"));
    // e uma concessão irrestrita simplesmente não tem bloco de condição
    var criarTurma = MAPPER.readTree(json).get("statements").findValues("sid").stream()
        .anyMatch(n -> n.asText().equals("ALLOW:CRIAR_TURMA:TURMA"));
    assertTrue(criarTurma);
  }

  @Test
  void aRotaDePoliticasESoDoAdministrador() {
    test((server, client) -> {
      criar2Professores2Alunos(client);
      assertEquals(200, GET(client, "/iam/politicas", ADM_ID).code());
      // a política diz quem alcança o quê: é informação de administração
      assertEquals(403, GET(client, "/iam/politicas", PROF1_ID).code());
      assertEquals(403, GET(client, "/iam/politicas", ALUNO1_ID).code());
      assertEquals(401, GET_ANONIMO(client, "/iam/politicas").code());
    });
  }

  /** Ida e volta preserva a decisão, cláusula a cláusula, cenário a cenário. */
  @Test
  void serializarEDesserializarNaoMudaNenhumaDecisao() throws Exception {
    var auth = SecurityContext.getInstance();
    auth.reset();

    var prof = new User("prof");
    var aluno = new User("aluno");
    var estranho = new User("estranho");
    MembershipManager.link(prof, auth.getProfessores());
    MembershipManager.link(aluno, auth.getAlunos());

    var turma = new Turma("POO", prof);
    turma.adicionarAluno(aluno);
    var postDoAluno = new Post("t", "c", aluno, turma);
    var comentarioDoProf = new Comentario("oi", prof, postDoAluno);

    List<User> principais = List.of(prof, aluno, estranho, auth.getAdmin());
    List<Resource> recursos = List.of(turma, postDoAluno, comentarioDoProf, prof, aluno);

    var contextos = new ArrayList<RequestContext>();
    for (User u : principais)
      for (Resource r : recursos)
        contextos.add(ContextResolver.padrao().resolver(u, r));

    var todas = new ArrayList<Statement>();
    todas.addAll(auth.getAdmin().getStatements());
    todas.addAll(auth.getProfessores().getStatements());
    todas.addAll(auth.getAlunos().getStatements());
    assertFalse(todas.isEmpty());

    var comparacoes = 0;
    for (Statement statement : todas) {
      var original = statement.getCondition();
      // ida e volta por texto de verdade: o documento é serializado e
      // reinterpretado, não apenas convertido de árvore para árvore
      var texto = MAPPER.writeValueAsString(ConditionJson.escrever(original));
      var relido = ConditionJson.ler(MAPPER.readValue(texto, Object.class));

      for (RequestContext ctx : contextos) {
        assertEquals(original.avaliar(ctx), relido.avaliar(ctx),
            "a condição " + original + " mudou de resposta ao voltar do JSON");
        comparacoes++;
      }
    }
    // garante que o laço realmente exercitou o conjunto todo
    assertTrue(comparacoes > 500, "poucas comparações: " + comparacoes);
  }
}
