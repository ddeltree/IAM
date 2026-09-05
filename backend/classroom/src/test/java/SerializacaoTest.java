import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.ObjectMapper;

import poo.Main;
import poo.api.IamMixins;
import poo.classroom.Post;
import poo.classroom.Turma;
import poo.classroom.iam.SecurityContext;
import poo.iam.MembershipManager;
import poo.iam.User;

/**
 * O único ponto do sistema onde uma regressão não é pega pelo compilador.
 *
 * Enquanto o núcleo carregava as anotações do Jackson, esconder grupos,
 * políticas e a corrente de pais era garantido pela classe. Agora quem garante
 * é o {@link IamMixins}, e esquecer de registrá-lo não quebra nenhuma
 * compilação: a serialização entra em recursão infinita em produção, ou vaza a
 * política de todo usuário embutido numa resposta.
 */
class SerializacaoTest {

  private static final ObjectMapper MAPPER = IamMixins.aplicar(new ObjectMapper());

  @BeforeEach
  void limpar() {
    Main.resetState();
  }

  @Test
  void umaTurmaPovoadaNaoLevaOMundoJunto() throws Exception {
    var auth = SecurityContext.getInstance();
    var professor = new User("Professor");
    var aluno = new User("Aluno");
    MembershipManager.link(professor, auth.getProfessores());
    MembershipManager.link(aluno, auth.getAlunos());

    var turma = new Turma("Cálculo", professor);
    turma.adicionarAluno(aluno);
    turma.adicionarPost(new Post("Aviso", "bom dia", aluno, turma));

    var json = MAPPER.writeValueAsString(turma);

    // a corrente de pais é para as condições lerem, não para sair na resposta:
    // sem escondê-la, post -> turma -> post entra em recursão
    assertFalse(json.contains("\"pai\""), "a corrente de pais vazou: " + json);

    // política e grupos são detalhe interno da autorização, e um usuário
    // aparece embutido em quase toda resposta (o autor de um post, aqui)
    for (String vazamento : List.of("\"groups\"", "\"statements\"",
        "\"inlinePermissions\"", "\"deniedPermissions\"", "\"users\""))
      assertFalse(json.contains(vazamento), vazamento + " vazou: " + json);

    // e o que deve aparecer continua aparecendo
    assertTrue(json.contains("\"Cálculo\""));
    assertTrue(json.contains("\"professorResponsavel\""));
  }
}
