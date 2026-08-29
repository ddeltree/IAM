import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.junit.jupiter.api.Test;

import poo.classroom.Comentario;
import poo.classroom.Post;
import poo.classroom.Turma;
import poo.classroom.iam.ClassroomPermission;
import poo.classroom.iam.SecurityContext;
import poo.iam.Group;
import poo.iam.MembershipManager;
import poo.iam.PrincipalDirectory;
import poo.iam.Resource;
import poo.iam.User;
import poo.iam.query.PolicyQuery;

/**
 * A consulta ao contrário.
 *
 * O teste central é a concordância: a poda por extração de restrição e a
 * varredura completa têm de devolver exatamente o mesmo conjunto. Enquanto isso
 * valer, a poda é só desempenho — e um erro nela nunca vira acesso indevido.
 */
public class QuemPodeTest extends ApiFixture {

  private record Cenario(PolicyQuery consulta, Turma turma, Post postDoAluno,
      Comentario comentarioDoProf, User prof, User outroProf, User aluno, User estranho) {
  }

  private static Cenario montar() {
    var auth = SecurityContext.getInstance();
    auth.reset();

    var prof = new User("prof");
    var outroProf = new User("outro prof");
    var aluno = new User("aluno");
    var outroAluno = new User("outro aluno");
    var estranho = new User("estranho");
    MembershipManager.link(prof, auth.getProfessores());
    MembershipManager.link(outroProf, auth.getProfessores());
    MembershipManager.link(aluno, auth.getAlunos());
    MembershipManager.link(outroAluno, auth.getAlunos());

    var turma = new Turma("POO", prof);
    turma.adicionarAluno(aluno);
    var post = new Post("t", "c", aluno, turma);
    var comentario = new Comentario("oi", prof, post);

    var todos = List.of(prof, outroProf, aluno, outroAluno, estranho, auth.getAdmin());
    PrincipalDirectory diretorio = new PrincipalDirectory() {
      @Override
      public Collection<User> usuarios() {
        return todos;
      }

      @Override
      public Collection<Group> grupos() {
        return List.of(auth.getProfessores(), auth.getAlunos());
      }
    };

    return new Cenario(new PolicyQuery(diretorio), turma, post, comentario,
        prof, outroProf, aluno, estranho);
  }

  @Test
  void quemExcluiOPostDoAluno() {
    var c = montar();
    var admin = SecurityContext.getInstance().getAdmin();

    var ids = idsDe(c.consulta().quemPode(
        ClassroomPermission.EXCLUIR_POST.get(), c.postDoAluno()).principais);

    // o autor, o professor responsável e a moderação do sistema
    assertEquals(List.of(c.prof().getId(), c.aluno().getId(), admin.getId()).stream().sorted().toList(),
        ids);
  }

  @Test
  void quemEditaOPostDoAluno() {
    var c = montar();
    var admin = SecurityContext.getInstance().getAdmin();

    var ids = idsDe(c.consulta().quemPode(
        ClassroomPermission.EDITAR_POST.get(), c.postDoAluno()).principais);

    // editar é do autor: o professor responsável fica de fora
    assertEquals(List.of(c.aluno().getId(), admin.getId()).stream().sorted().toList(), ids);
  }

  @Test
  void quemMatriculaNaTurma() {
    var c = montar();
    var ids = idsDe(c.consulta().quemPode(
        ClassroomPermission.MATRICULAR_ALUNO.get(), c.turma()).principais);

    // só o professor responsável — nem o outro professor, nem o administrador
    assertEquals(List.of(c.prof().getId()), ids);
  }

  /** A propriedade que sustenta a poda. */
  @Test
  void podaEVarreduraConcordamSempre() {
    var c = montar();
    List<Resource> recursos = List.of(c.turma(), c.postDoAluno(), c.comentarioDoProf(),
        c.prof(), c.aluno(), c.estranho());

    var comparacoes = 0;
    for (ClassroomPermission permissao : ClassroomPermission.values()) {
      for (Resource recurso : recursos) {
        var podado = idsDe(c.consulta().quemPode(permissao.get(), recurso).principais);
        var varrido = idsDe(c.consulta().quemPodeVarrendo(permissao.get(), recurso));
        assertEquals(varrido, podado,
            "divergência em " + permissao + " sobre " + recurso.getType() + "/" + recurso.getId());
        comparacoes++;
      }
    }
    assertTrue(comparacoes > 100, "poucas comparações: " + comparacoes);
  }

  @Test
  void aPodaRealmenteEvitaTrabalho() {
    var c = montar();
    // matricular tem condição concreta sobre a turma: dá para saber quem é
    var resultado = c.consulta().quemPode(
        ClassroomPermission.MATRICULAR_ALUNO.get(), c.turma());

    assertTrue(resultado.podou);
    assertTrue(resultado.avaliados < resultado.conhecidos,
        "avaliou " + resultado.avaliados + " de " + resultado.conhecidos);
  }

  private static List<String> idsDe(List<User> users) {
    var ids = new ArrayList<String>();
    users.forEach(u -> ids.add(u.getId()));
    return ids.stream().sorted().toList();
  }
}
