import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Collection;
import java.util.List;

import org.junit.jupiter.api.Test;

import poo.classroom.Turma;
import poo.classroom.iam.ClassroomPermission;
import poo.classroom.iam.ClassroomSqlMapping;
import poo.classroom.iam.SecurityContext;
import poo.iam.AccessResolver;
import poo.iam.Group;
import poo.iam.MembershipManager;
import poo.iam.spi.PrincipalDirectory;
import poo.iam.User;
import poo.iam.query.PolicyQuery;
import poo.iam.query.SqlWhereRenderer;

/**
 * O dual da consulta reversa: sobre o que este usuário alcança.
 *
 * O ponto do desenho está no último teste — o mesmo filtro derivado da política
 * vira um predicado em memória com um visitante e uma cláusula WHERE com outro,
 * sem que a política mude.
 */
public class OndePossoTest extends ApiFixture {

  private static PolicyQuery consulta(List<User> usuarios) {
    var auth = SecurityContext.getInstance();
    return new PolicyQuery(new PrincipalDirectory() {
      @Override
      public Collection<User> usuarios() {
        return usuarios;
      }

      @Override
      public Collection<Group> grupos() {
        return List.of(auth.getProfessores(), auth.getAlunos());
      }
    });
  }

  @Test
  void oFiltroDoProfessorEDoAlunoSaoDiferentes() {
    var auth = SecurityContext.getInstance();
    auth.reset();
    var prof = new User("prof");
    var aluno = new User("aluno");
    MembershipManager.link(prof, auth.getProfessores());
    MembershipManager.link(aluno, auth.getAlunos());
    var q = consulta(List.of(prof, aluno));

    var doProf = q.ondePosso(prof, ClassroomPermission.LISTAR_TURMAS.get());
    var doAluno = q.ondePosso(aluno, ClassroomPermission.LISTAR_TURMAS.get());

    assertEquals("turma:professorId = " + prof.getId(), doProf.toString());
    assertEquals("turma:alunoIds contém " + aluno.getId(), doAluno.toString());

    // o administrador alcança tudo, e o filtro diz isso
    assertEquals("tudo",
        q.ondePosso(auth.getAdmin(), ClassroomPermission.LISTAR_TURMAS.get()).toString());
  }

  @Test
  void filtrarConcordaComOMotor() {
    var auth = SecurityContext.getInstance();
    auth.reset();
    var prof = new User("prof");
    var outroProf = new User("outro");
    var aluno = new User("aluno");
    MembershipManager.link(prof, auth.getProfessores());
    MembershipManager.link(outroProf, auth.getProfessores());
    MembershipManager.link(aluno, auth.getAlunos());

    var minha = new Turma("POO", prof);
    minha.adicionarAluno(aluno);
    var alheia = new Turma("Cálculo", outroProf);
    var turmas = List.of(minha, alheia);

    var q = consulta(List.of(prof, outroProf, aluno, auth.getAdmin()));
    var permissao = ClassroomPermission.LISTAR_TURMAS.get();

    for (User quem : List.of(prof, outroProf, aluno, auth.getAdmin())) {
      var filtrado = q.filtrar(quem, permissao, turmas).stream().map(Turma::getId).sorted().toList();
      var peloMotor = turmas.stream()
          .filter(t -> AccessResolver.isAllowed(quem, permissao, t))
          .map(Turma::getId).sorted().toList();
      assertEquals(peloMotor, filtrado, "divergência para " + quem.getName());
    }
  }

  /** A mesma restrição, dois destinos. */
  @Test
  void oMesmoFiltroViraPredicadoEViraSql() {
    var auth = SecurityContext.getInstance();
    auth.reset();
    var prof = new User("prof");
    var aluno = new User("aluno");
    MembershipManager.link(prof, auth.getProfessores());
    MembershipManager.link(aluno, auth.getAlunos());
    var q = consulta(List.of(prof, aluno));
    var mapeamento = new ClassroomSqlMapping();

    var sqlProf = SqlWhereRenderer.render(
        q.ondePosso(prof, ClassroomPermission.LISTAR_TURMAS.get()), mapeamento);
    assertEquals("turma.professor_id = '" + prof.getId() + "'", sqlProf);

    var sqlAluno = SqlWhereRenderer.render(
        q.ondePosso(aluno, ClassroomPermission.LISTAR_TURMAS.get()), mapeamento);
    assertTrue(sqlAluno.startsWith("EXISTS (SELECT 1 FROM matricula m"), sqlAluno);
    assertTrue(sqlAluno.contains("m.aluno_id = '" + aluno.getId() + "'"), sqlAluno);

    // sem restrição nenhuma, a cláusula não filtra
    assertEquals("1=1", SqlWhereRenderer.render(
        q.ondePosso(auth.getAdmin(), ClassroomPermission.LISTAR_TURMAS.get()), mapeamento));
  }
}
