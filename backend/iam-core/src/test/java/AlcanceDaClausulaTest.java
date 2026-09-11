import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import poo.iam.Action;
import poo.iam.Effect;
import poo.iam.Iam;
import poo.iam.IamFactory;
import poo.iam.Permission;
import poo.iam.Resource;
import poo.iam.ResourceType;
import poo.iam.Statement;
import poo.iam.User;
import poo.iam.condition.Condition;

/**
 * O que uma cláusula alcança, antes de qualquer condição.
 *
 * Há dois formatos de pedido, e o segundo é fácil de esquecer: o que tem alvo — "pode
 * apagar <em>este</em> arquivo?" — e o que não tem, que é como se pedem as ações de
 * criar e de listar. Recurso nulo casava com qualquer padrão, e daí uma concessão presa a
 * um objeto autorizava a ação que não mira objeto nenhum: quem recebeu
 * {@code CRIAR ARQUIVO/a9} para poder mexer num arquivo passava a poder criar arquivos.
 *
 * A regra que fecha isso: <b>pedido sem alvo é alcançado só por cláusula que não
 * restringe o id</b>. É o que mantém o padrão de recurso significando o que ele diz nos
 * dois formatos de pedido.
 */
class AlcanceDaClausulaTest {

  enum Acao implements Action {
    CRIAR, LER
  }

  enum Tipo implements ResourceType {
    ARQUIVO
  }

  record Arquivo(String id) implements Resource {
    public ResourceType getType() {
      return Tipo.ARQUIVO;
    }

    public String getId() {
      return id;
    }
  }

  static final Permission CRIAR = new Permission(Acao.CRIAR, Tipo.ARQUIVO);
  static final Permission LER = new Permission(Acao.LER, Tipo.ARQUIVO);

  private final Iam iam = IamFactory.novo().construir();

  /** Quem tem a cláusula, seja qual for o padrão de recurso dela. */
  private User comAClausula(String padrao, Acao acao) {
    var ana = new User("1", "Ana");
    ana.add(Statement.de(Effect.ALLOW, acao.name(), padrao, Condition.SEMPRE));
    return ana;
  }

  @Test
  void umaClausulaPresaAUmaInstanciaNaoAtendePedidoSemAlvo() {
    var ana = comAClausula("ARQUIVO/a9", Acao.CRIAR);

    assertFalse(iam.motor().isAllowed(ana, CRIAR, null),
        "a cláusula fala do a9; criar não fala de arquivo nenhum");
    assertTrue(iam.motor().isAllowed(ana, CRIAR, new Arquivo("a9")),
        "e sobre o a9 ela continua valendo");
  }

  @Test
  void nemMesmoComCuringaNoMeioDoId() {
    // "ARQUIVO/a*" restringe o id tanto quanto "ARQUIVO/a9": os dois nomeiam
    // instâncias, e um pedido sem alvo não tem id para nomear
    var ana = comAClausula("ARQUIVO/a*", Acao.CRIAR);

    assertFalse(iam.motor().isAllowed(ana, CRIAR, null));
    assertTrue(iam.motor().isAllowed(ana, CRIAR, new Arquivo("a9")));
  }

  @Test
  void aClausulaDeTipoAtendeOsDoisFormatosDePedido() {
    // é o padrão que ResourcePattern.deTipo produz, e o que grantPermission usa:
    // se ele deixasse de alcançar o pedido sem alvo, nada mais criaria nada
    var ana = comAClausula("ARQUIVO", Acao.CRIAR);

    assertTrue(iam.motor().isAllowed(ana, CRIAR, null), "ARQUIVO não restringe id nenhum");
    assertTrue(iam.motor().isAllowed(ana, CRIAR, new Arquivo("a9")));
  }

  @Test
  void oCuringaTotalContinuaAlcancandoTudo() {
    var ana = comAClausula("*", Acao.CRIAR);

    assertTrue(iam.motor().isAllowed(ana, CRIAR, null));
    assertTrue(iam.motor().isAllowed(ana, CRIAR, new Arquivo("a9")));
  }

  @Test
  void aConcessaoDeSempreSegueValendoSobreAsInstancias() {
    // o caminho comum, que nada disto pode ter mexido
    var ana = new User("1", "Ana");
    ana.grantPermission(LER);

    assertTrue(iam.motor().isAllowed(ana, LER, new Arquivo("a9")));
    assertTrue(iam.motor().isAllowed(ana, LER, null));
  }
}
