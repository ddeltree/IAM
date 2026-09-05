import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import poo.iam.Action;
import poo.iam.Effect;
import poo.iam.IamFactory;
import poo.iam.Permission;
import poo.iam.Resource;
import poo.iam.ResourceType;
import poo.iam.Role;
import poo.iam.Statement;
import poo.iam.User;
import poo.iam.condition.Condition;
import poo.iam.spi.AttributeProvider;

/**
 * Um domínio cujo vocabulário nasce em tempo de execução.
 *
 * O classroom declara tudo em enums e nunca pressiona isto: os tipos existem
 * antes do programa rodar, e um {@code AttributeProvider} por tipo é registrado
 * na inicialização. Uma aplicação em que o <em>usuário</em> cria os tipos —
 * um console de IAM, por exemplo — precisa de outras três coisas, e é por elas
 * que este teste existe.
 */
class DominioDinamicoTest {

  /** Tipos e ações que são só um nome, criados quando alguém os digita. */
  record TipoLivre(String name) implements ResourceType {
  }

  record AcaoLivre(String name) implements Action {
  }

  /** Um recurso que carrega os próprios atributos. */
  static final class RecursoLivre implements Resource {
    private final String tipo;
    private final String id;
    private final Map<String, List<String>> atributos = new LinkedHashMap<>();

    RecursoLivre(String tipo, String id) {
      this.tipo = tipo;
      this.id = id;
    }

    RecursoLivre com(String chave, String valor) {
      atributos.put(chave, List.of(valor));
      return this;
    }

    public ResourceType getType() {
      return new TipoLivre(tipo);
    }

    public String getId() {
      return id;
    }

    Map<String, List<String>> getAtributos() {
      return atributos;
    }
  }

  /** Um provedor só, para todos os tipos: cada recurso já sabe os seus. */
  static final AttributeProvider DE_QUALQUER_TIPO = new AttributeProvider() {
    public ResourceType tipo() {
      return new TipoLivre("*"); // ignorado quando registrado como padrão
    }

    public Map<String, List<String>> atributosDe(Resource r) {
      return ((RecursoLivre) r).getAtributos();
    }
  };

  @Test
  void umProvedorPadraoAtendeTiposQueAindaNaoExistiam() {
    var iam = IamFactory.novo().construir();
    iam.contexto().registrarPadrao(DE_QUALQUER_TIPO);

    var ana = new User("1", "Ana");
    // a política é escrita antes de o tipo BUCKET existir na cabeça de alguém
    ana.add(Statement.de(Effect.ALLOW, "LER", "*",
        Condition.igual("recurso:dono", "${principal:id}")));

    var dela = new RecursoLivre("BUCKET", "relatorios").com("dono", "1");
    var doOutro = new RecursoLivre("BUCKET", "folha").com("dono", "2");
    var ler = new Permission(new AcaoLivre("LER"), new TipoLivre("BUCKET"));

    assertTrue(iam.motor().isAllowed(ana, ler, dela));
    assertFalse(iam.motor().isAllowed(ana, ler, doOutro));

    // e um tipo completamente diferente, sem nada registrado para ele
    var doc = new RecursoLivre("DOCUMENTO", "d1").com("dono", "1");
    var lerDoc = new Permission(new AcaoLivre("LER"), new TipoLivre("DOCUMENTO"));
    assertTrue(iam.motor().isAllowed(ana, lerDoc, doc),
        "sem provedor padrão, seria preciso registrar um por tipo criado");
  }

  @Test
  void umaClausulaSeRemoveUmaAUma() {
    var ana = new User("1", "Ana");
    var comCuringa = Statement.allow("*", "*");
    var especifica = Statement.allow(new Permission(new AcaoLivre("LER"), new TipoLivre("BUCKET")));
    ana.add(comCuringa);
    ana.add(especifica);
    assertEquals(2, ana.getStatements().size());

    // revoke(Permission) não serve aqui: apagaria todas as concessões daquela
    // permissão, e nem alcança a cláusula com curinga
    assertTrue(ana.remover(comCuringa));
    assertEquals(1, ana.getStatements().size());
    assertTrue(ana.getStatements().contains(especifica));

    assertTrue(ana.removerPorSid(especifica.getSid()));
    assertEquals(0, ana.getStatements().size());
    assertFalse(ana.removerPorSid("nao-existe"));
  }

  @Test
  void aConfiancaDeUmPapelSeEdita() {
    var iam = IamFactory.novo().construir();
    var ana = new User("1", "Ana");
    var bruno = new User("2", "Bruno");

    var auditor = new Role("Auditor");
    auditor.confiaEm(Condition.igual("principal:id", "1"));
    auditor.confiaEm(Condition.igual("principal:id", "2"));
    assertEquals(2, auditor.getConfiancaStatements().size());

    assertTrue(iam.papeis().podeAssumir(ana, auditor).permitido());
    assertTrue(iam.papeis().podeAssumir(bruno, auditor).permitido());

    // tirar a confiança de um sem reconstruir o papel — reconstruí-lo perderia
    // as políticas anexadas e as cláusulas próprias
    var doBruno = auditor.getConfiancaStatements().stream()
        .filter(s -> s.getCondition().toString().contains("2"))
        .findFirst().orElseThrow();
    assertTrue(auditor.deixaDeConfiar(doBruno.getSid()));

    assertTrue(iam.papeis().podeAssumir(ana, auditor).permitido());
    assertFalse(iam.papeis().podeAssumir(bruno, auditor).permitido());
  }
}
