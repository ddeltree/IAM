package poo.classroom.iam;

import java.io.IOException;

import com.fasterxml.jackson.databind.ObjectMapper;

import poo.iam.Effect;
import poo.iam.Group;
import poo.iam.Iam;
import poo.iam.IamFactory;
import poo.iam.Statement;
import poo.iam.User;
import poo.iam.document.PolicyRepository;

/**
 * Os principais desta aplicação e o componente de autorização que os avalia.
 *
 * A <b>política</b> não está aqui: está em {@code politica-padrao.json}, e é lá
 * que se lê quem pode o quê. Este arquivo monta o motor, cria os três
 * principais fixos e anexa a cada um a política nomeada correspondente.
 *
 * Os três papéis são fixos porque esta aplicação decidiu assim, e não porque o
 * núcleo peça: para ele, ADMIN é um usuário e Professores é um grupo, ambos
 * com uma política anexada — nada os distingue de qualquer outro.
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

  /**
   * Anexa a política padrão, carregada do documento.
   *
   * Isto era ~50 chamadas a {@code grantPermission} escritas em Java. Elas
   * liam bem — cada linha era uma cláusula legível —, mas a política morava no
   * código: mudar quem pode o quê exigia recompilar, e o
   * {@code GET /iam/politicas} era um relatório sobre o código, não a fonte.
   * Agora o código é o motor e o documento é a política.
   *
   * As três são políticas <em>nomeadas</em>, anexadas em vez de copiadas — é a
   * managed policy da AWS. Mudar "Professor" muda o acesso de todos os
   * professores de uma vez, e sem precisar tocar em cada principal.
   */
  private void configurarPermissoesPadrao() {
    var politicas = Documento.POLITICAS;
    admin.anexar(politicas.porNome("Administracao"));
    professores.anexar(politicas.porNome("Professor"));
    alunos.anexar(politicas.porNome("Aluno"));
  }

  /**
   * O documento é lido uma vez, e num holder aninhado de propósito.
   *
   * Um {@code static final} aqui seria inicializado na ordem em que aparece no
   * arquivo, e o singleton {@code instance} — que também é estático, e que
   * chama isto no construtor — já mordeu essa ordem antes; o comentário
   * "declarado antes do singleton" lá em cima é a cicatriz. Um holder é
   * carregado no primeiro uso, então a ordem dos campos deixa de importar.
   *
   * Se o documento estiver malformado ou ausente, a aplicação não sobe — e é o
   * que se quer: subir com a política vazia significaria todo mundo barrado,
   * sem nada dizendo por quê.
   */
  private static final class Documento {
    static final PolicyRepository POLITICAS = carregarPoliticas();
  }

  private static PolicyRepository carregarPoliticas() {
    try (var entrada = SecurityContext.class.getResourceAsStream(DOCUMENTO)) {
      if (entrada == null)
        throw new IllegalStateException("Documento de política não encontrado: " + DOCUMENTO);
      return PolicyRepository.deDocumento(new ObjectMapper().readValue(entrada, Object.class));
    } catch (IOException e) {
      throw new IllegalStateException("Não consegui ler " + DOCUMENTO, e);
    }
  }

  private static final String DOCUMENTO = "/politica-padrao.json";

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
