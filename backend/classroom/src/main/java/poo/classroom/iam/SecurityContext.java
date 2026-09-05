package poo.classroom.iam;

import static poo.classroom.iam.ClassroomConditions.*;

import poo.iam.condition.Condition;
import static poo.classroom.iam.ClassroomPermission.*;

import poo.iam.Effect;
import poo.iam.Group;
import poo.iam.Iam;
import poo.iam.IamFactory;
import poo.iam.Statement;
import poo.iam.User;

/**
 * A política padrão desta aplicação: quem é cada papel e o que cada um recebe.
 *
 * Cada linha abaixo é o equivalente a um statement de policy da AWS — permissão
 * mais a condição sob a qual ela vale. É por isso que o administrador aparece
 * concedendo as mesmas permissões dos outros, só que sem condição: ele modera
 * qualquer turma, enquanto o professor só mexe na dele.
 */
public class SecurityContext {

  // declarado antes do singleton: a construção dele depende destes campos
  private static final SecurityContext instance = new SecurityContext(); // Singleton

  private final User admin;
  private final Group alunos;
  private final Group professores;
  private final Iam iam;

  private SecurityContext() {
    this.admin = new User("ADMIN");
    this.alunos = new Group("Alunos");
    this.professores = new Group("Professores");
    this.iam = montarIam();
    configurarPermissoesPadrao();
  }

  /**
   * O componente de autorização desta aplicação.
   *
   * Ele é montado uma vez e vive aqui. Antes o motor era estático no núcleo e
   * lia um resolvedor global — o que só funcionava porque há um sistema só
   * neste processo. Agora o classroom é dono da instância dele, e o núcleo
   * voltou a poder ser usado duas vezes no mesmo JVM sem que uma configuração
   * apague a outra.
   */
  public Iam iam() {
    return iam;
  }

  public static SecurityContext getInstance() {
    return instance;
  }

  /**
   * Devolve o contexto ao estado inicial: grupos vazios e política padrão.
   * Usado pelos testes, que compartilham o mesmo singleton entre cenários.
   */
  public void reset() {
    admin.clearPermissions();
    alunos.clearPermissions();
    alunos.clearUsers();
    professores.clearPermissions();
    professores.clearUsers();
    configurarPermissoesPadrao();
  }

  /**
   * As condições leem chaves como {@code turma:professorId}; sem os provedores
   * de atributo, todas elas silenciariam para falso e tudo viraria 403.
   *
   * O {@code reset()} não refaz isto: os provedores são a adaptação do domínio
   * ao núcleo, e não estado de cenário — o que os testes precisam reiniciar é a
   * política, não a forma de ler uma turma.
   */
  private static Iam montarIam() {
    return IamFactory.novo()
        .atributos(ClassroomAttributes.todos())
        .catalogo(ClassroomPermission.CATALOGO)
        .construir();
  }

  private void configurarPermissoesPadrao() {
    configurarAdmin();
    configurarProfessores();
    configurarAlunos();
  }

  /**
   * O administrador é um moderador: enxerga e corrige tudo, sem restrição de
   * turma — e por isso mesmo não cria turma, post, atividade nem comentário.
   *
   * Isto eram quinze concessões enumeradas, uma por ação, e cada ação nova
   * exigia lembrar de acrescentá-lo — sendo que esquecer não acusava nada, só
   * deixava a política errada em silêncio. Agora são duas cláusulas: pode
   * tudo, menos criar.
   */
  private void configurarAdmin() {
    admin.add(Statement.allow("*", "*"));

    // As três negações abaixo é que dizem o que ele *não* é. Negação explícita
    // vence qualquer concessão, inclusive o curinga acima — é exatamente para
    // isso que a ordem de avaliação da AWS existe.

    // não cria conteúdo: modera o que os outros criam. As quatro ações de criar
    // conteúdo são todas sobre a TURMA, então uma cláusula dá conta — e
    // CRIAR_PROFESSOR, que é sobre USUARIO, continua valendo
    admin.add(Statement.de(Effect.DENY, "CRIAR_*", "TURMA", Condition.SEMPRE));
    admin.add(Statement.de(Effect.DENY, "CRIAR_ALUNO", "*", Condition.SEMPRE));

    // nem monta turma: quem matricula e desmatricula é o professor dela
    admin.add(Statement.de(Effect.DENY, "*MATRICULAR_ALUNO", "*", Condition.SEMPRE));
  }

  /** O professor manda na turma dele, e em nenhuma outra. */
  private void configurarProfessores() {
    professores.grantPermission(LISTAR_TURMAS.get(), PROFESSOR_RESPONSAVEL);
    professores.grantPermission(VER_TURMA.get(), PARTICIPANTE);
    professores.grantPermission(LISTAR_ATIVIDADES.get(), PARTICIPANTE);
    professores.grantPermission(LISTAR_POSTS.get(), PARTICIPANTE);
    professores.grantPermission(LISTAR_COMENTARIOS.get(), PARTICIPANTE);
    professores.grantPermission(LISTAR_PARTICIPANTES.get(), PARTICIPANTE);
    professores.grantPermission(VER_PERFIL.get(), PROPRIO_USUARIO);

    professores.grantPermission(CRIAR_TURMA.get());
    professores.grantPermission(CRIAR_ALUNO.get());
    professores.grantPermission(CRIAR_ATIVIDADE.get(), PROFESSOR_RESPONSAVEL);
    professores.grantPermission(CRIAR_POST.get(), PARTICIPANTE);
    professores.grantPermission(CRIAR_COMENTARIO.get(), PARTICIPANTE);

    professores.grantPermission(EDITAR_TURMA.get(), PROFESSOR_RESPONSAVEL);
    professores.grantPermission(EDITAR_ATIVIDADE.get(), PROFESSOR_RESPONSAVEL);
    professores.grantPermission(EDITAR_USUARIO.get(), PROPRIO_USUARIO);
    // editar conteúdo alheio, não: corrigir texto dos outros é do administrador
    professores.grantPermission(EDITAR_POST.get(), AUTOR);
    professores.grantPermission(EDITAR_COMENTARIO.get(), AUTOR);

    professores.grantPermission(EXCLUIR_TURMA.get(), PROFESSOR_RESPONSAVEL);
    professores.grantPermission(EXCLUIR_ATIVIDADE.get(), AUTOR);
    professores.grantPermission(EXCLUIR_USUARIO.get(), PROPRIO_USUARIO);
    // já apagar, sim: é a moderação da própria turma
    professores.grantPermission(EXCLUIR_POST.get(), PROFESSOR_RESPONSAVEL.ou(AUTOR));
    professores.grantPermission(EXCLUIR_COMENTARIO.get(), PROFESSOR_RESPONSAVEL.ou(AUTOR));

    professores.grantPermission(MATRICULAR_ALUNO.get(), PROFESSOR_RESPONSAVEL);
    professores.grantPermission(DESMATRICULAR_ALUNO.get(), PROFESSOR_RESPONSAVEL);
  }

  /** O aluno participa: publica e comenta, e só mexe no que é dele. */
  private void configurarAlunos() {
    alunos.grantPermission(LISTAR_TURMAS.get(), ALUNO_MATRICULADO);
    alunos.grantPermission(VER_TURMA.get(), PARTICIPANTE);
    alunos.grantPermission(LISTAR_ATIVIDADES.get(), PARTICIPANTE);
    alunos.grantPermission(LISTAR_POSTS.get(), PARTICIPANTE);
    alunos.grantPermission(LISTAR_COMENTARIOS.get(), PARTICIPANTE);
    alunos.grantPermission(LISTAR_PARTICIPANTES.get(), PARTICIPANTE);
    alunos.grantPermission(VER_PERFIL.get(), PROPRIO_USUARIO);

    alunos.grantPermission(CRIAR_POST.get(), PARTICIPANTE);
    alunos.grantPermission(CRIAR_COMENTARIO.get(), PARTICIPANTE);

    alunos.grantPermission(EDITAR_POST.get(), AUTOR);
    alunos.grantPermission(EDITAR_COMENTARIO.get(), AUTOR);
    alunos.grantPermission(EDITAR_USUARIO.get(), PROPRIO_USUARIO);

    alunos.grantPermission(EXCLUIR_POST.get(), AUTOR);
    alunos.grantPermission(EXCLUIR_COMENTARIO.get(), AUTOR);
    alunos.grantPermission(EXCLUIR_USUARIO.get(), PROPRIO_USUARIO);
  }

  public User getAdmin() {
    return admin;
  }

  public Group getAlunos() {
    return alunos;
  }

  public Group getProfessores() {
    return professores;
  }

  public boolean isProfessor(User user) {
    return user != null && user.getGroups().contains(professores);
  }

  public boolean isAluno(User user) {
    return user != null && user.getGroups().contains(alunos);
  }

  public boolean isAdmin(User user) {
    return user != null && user.getId().equals(admin.getId());
  }

  public boolean isAdmin(String uid) {
    return uid.equals(admin.getId());
  }
}
