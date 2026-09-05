import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import poo.classroom.Post;
import poo.classroom.Turma;
import poo.classroom.iam.ClassroomConditions;
import poo.classroom.iam.SecurityContext;
import poo.iam.PermissionCondition;
import poo.iam.User;
import poo.iam.condition.CondicaoOpaca;
import poo.iam.condition.Condition;

/**
 * As condições como dado: leem chaves de contexto em vez de navegar objetos, e
 * é por isso que a política pode ser lida, e não só executada.
 */
public class CondicoesTest extends ApiFixture {

  private static Post postDe(User professor, User autor) {
    var turma = new Turma("POO", professor);
    return new Post("titulo", "corpo", autor, turma);
  }

  @Test
  void oContextoSobeACorrenteDePais() {
    var prof = new User("prof");
    var aluno = new User("aluno");
    var post = postDe(prof, aluno);

    var ctx = SecurityContext.getInstance().iam().contexto().resolver(aluno, post);

    // do próprio post
    assertEquals(java.util.List.of(aluno.getId()), ctx.get("post:autorId"));
    // e da turma que o contém, sem ninguém escrever essa navegação
    assertEquals(java.util.List.of(prof.getId()), ctx.get("turma:professorId"));
    // o alvo também aparece com o apelido genérico
    assertEquals(java.util.List.of(aluno.getId()), ctx.get("recurso:autorId"));
    assertEquals(java.util.List.of("POST"), ctx.get("recurso:tipo"));
  }

  @Test
  void aVariavelDePoliticaSeRefereAQuemEstaPedindo() {
    var prof = new User("prof");
    var aluno = new User("aluno");
    var post = postDe(prof, aluno);

    // uma cláusula só, que serve a qualquer usuário
    assertTrue(ClassroomConditions.AUTOR.avaliar(
        SecurityContext.getInstance().iam().contexto().resolver(aluno, post)));
    assertFalse(ClassroomConditions.AUTOR.avaliar(
        SecurityContext.getInstance().iam().contexto().resolver(prof, post)));
  }

  @Test
  void condicaoSabeDizerQuaisChavesLe() {
    assertEquals(java.util.Set.of("turma:professorId"),
        ClassroomConditions.PROFESSOR_RESPONSAVEL.chaves());
    assertTrue(ClassroomConditions.PARTICIPANTE.chaves()
        .containsAll(java.util.Set.of("turma:professorId", "turma:alunoIds", "principal:groups")));
  }

  @Test
  void operadorDeConjuntoCasaQualquerValorDaChave() {
    var prof = new User("prof");
    var aluno = new User("aluno");
    var turma = new Turma("POO", prof);
    turma.adicionarAluno(aluno);

    var doAluno = SecurityContext.getInstance().iam().contexto().resolver(aluno, turma);
    assertTrue(ClassroomConditions.ALUNO_MATRICULADO.avaliar(doAluno));

    var deFora = SecurityContext.getInstance().iam().contexto().resolver(new User("de fora"), turma);
    assertFalse(ClassroomConditions.ALUNO_MATRICULADO.avaliar(deFora));
  }

  /**
   * A ponte que permitiu migrar as condições uma de cada vez: continua
   * avaliando, mas nunca finge saber o que não sabe.
   */
  @Test
  void condicaoOpacaAvaliaMasNaoSeDeixaInspecionar() {
    PermissionCondition legado = ctx -> ctx.getPrincipal().equals(ctx.getRecurso());
    var opaca = new CondicaoOpaca(legado);
    var alguem = new User("alguem");

    assertTrue(opaca.avaliar(SecurityContext.getInstance().iam().contexto().resolver(alguem, alguem)));
    assertFalse(opaca.avaliar(SecurityContext.getInstance().iam().contexto().resolver(alguem, new User("outro"))));

    // não declara chave nenhuma: quem inspeciona recebe "não sei"
    assertTrue(opaca.chaves().isEmpty());
  }

  @Test
  void condicoesIguaisSaoIguaisPorEstrutura() {
    assertEquals(Condition.igual("recurso:id", "${principal:id}"),
        Condition.igual("recurso:id", "${principal:id}"));
    assertFalse(Condition.igual("recurso:id", "x")
        .equals(Condition.igual("recurso:id", "y")));
  }
}
