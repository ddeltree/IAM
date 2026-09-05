import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import poo.iam.Action;
import poo.iam.Effect;
import poo.iam.Group;
import poo.iam.Iam;
import poo.iam.IamFactory;
import poo.iam.MembershipManager;
import poo.iam.Permission;
import poo.iam.Resource;
import poo.iam.ResourceType;
import poo.iam.Statement;
import poo.iam.User;
import poo.iam.condition.Condition;
import poo.iam.spi.AttributeProvider;

/**
 * "Por que não?" — a pergunta que a decisão sozinha não responde.
 *
 * {@code Decisao} nomeia a cláusula que decidiu, e quando nada concede não tem
 * o que nomear: "nenhuma cláusula concede" não diz por que as que existem não
 * serviram. Quem está montando uma política precisa saber se errou o recurso
 * mirado, se a condição comparou os valores errados, ou se de fato não escreveu
 * cláusula nenhuma sobre aquela ação — três problemas diferentes com o mesmo
 * sintoma.
 */
class ExplicacaoTest {

  enum Acao implements Action {
    LER, ESCREVER
  }

  enum Tipo implements ResourceType {
    ARQUIVO
  }

  record Arquivo(String id, String donoId) implements Resource {
    public ResourceType getType() {
      return Tipo.ARQUIVO;
    }

    public String getId() {
      return id;
    }
  }

  static final Permission LER = new Permission(Acao.LER, Tipo.ARQUIVO);
  static final Permission ESCREVER = new Permission(Acao.ESCREVER, Tipo.ARQUIVO);

  private Iam iam;
  private User ana;
  private User bruno;
  private Arquivo dela;
  private Arquivo dele;

  @BeforeEach
  void montar() {
    ana = new User("1", "Ana");
    bruno = new User("2", "Bruno");
    var equipe = new Group("Equipe");
    MembershipManager.link(ana, equipe);
    MembershipManager.link(bruno, equipe);

    equipe.grantPermission(LER, Condition.igual("recurso:donoId", "${principal:id}"));
    // esta mira um arquivo específico: alcança o a1 e nenhum outro
    equipe.add(Statement.de(Effect.ALLOW, "ESCREVER", "ARQUIVO/a1", Condition.SEMPRE));

    dela = new Arquivo("a1", ana.getId());
    dele = new Arquivo("a2", bruno.getId());

    iam = IamFactory.novo().atributos(new AttributeProvider() {
      public ResourceType tipo() {
        return Tipo.ARQUIVO;
      }

      public Map<String, List<String>> atributosDe(Resource r) {
        return Map.of("donoId", List.of(((Arquivo) r).donoId()));
      }
    }).construir();
  }

  @Test
  void mostraOValorQueACondicaoComparou() {
    var exp = iam.motor().explicar(bruno, LER, dela);

    assertFalse(exp.getDecisao().permitido());
    assertEquals(1, exp.getClausulas().size(), "há uma cláusula falando sobre LER");

    var clausula = exp.getClausulas().get(0);
    assertTrue(clausula.alcancaORecurso(), "o padrão de recurso alcança");
    assertFalse(clausula.condicaoPassou(), "mas a condição não");

    // e aqui está o porquê, em vez de só "não passou"
    assertEquals(List.of("1"), exp.getContexto().get("recurso:donoId"));
    assertEquals(List.of("2"), exp.getContexto().get("principal:id"));
  }

  @Test
  void distingueRecursoErradoDeCondicaoErrada() {
    // a cláusula de ESCREVER mira ARQUIVO/a1; perguntando sobre o a2, ela nem
    // chega a ser avaliada — e confundir isso com "a condição não passou" faz
    // procurar erro no lugar errado
    var exp = iam.motor().explicar(ana, ESCREVER, dele);

    assertFalse(exp.getDecisao().permitido());
    var clausula = exp.getClausulas().get(0);
    assertFalse(clausula.alcancaORecurso(), "ela fala de ESCREVER, mas sobre outro arquivo");
    assertFalse(clausula.condicaoPassou());
    assertFalse(clausula.aplicaria());

    // sobre o arquivo que ela mira, aplica
    assertTrue(iam.motor().explicar(ana, ESCREVER, dela).getClausulas().get(0).aplicaria());
  }

  @Test
  void dizQuandoNenhumaClausulaMencionaAAcao() {
    var solitario = new User("9", "Ninguém");
    var exp = iam.motor().explicar(solitario, LER, dela);

    assertEquals(0, exp.getClausulas().size());
    assertEquals(0, exp.getClausulasAlcancadas());

    // e com política que não fala sobre a ação: o total denuncia
    var comOutras = new User("10", "Outro");
    comOutras.grantPermission(ESCREVER);
    var exp2 = iam.motor().explicar(comOutras, LER, dela);
    assertEquals(0, exp2.getClausulas().size(), "nenhuma fala sobre LER");
    assertEquals(1, exp2.getClausulasAlcancadas(), "mas ele tem uma cláusula");
  }

  @Test
  void marcaAClausulaQueDecidiu() {
    var exp = iam.motor().explicar(ana, LER, dela);

    assertTrue(exp.getDecisao().permitido());
    var decisivas = exp.getClausulas().stream().filter(c -> c.isDecisiva()).toList();
    assertEquals(1, decisivas.size());
    assertEquals("Equipe", decisivas.get(0).getOrigem());
  }

  @Test
  void aDecisaoDaExplicacaoESempreADoMotor() {
    // o invariante que impede a explicação de virar um segundo caminho de
    // decisão: ela pergunta ao motor, não recalcula. Se um dia alguém a
    // "otimizar" percorrendo a política por conta própria, os dois divergem no
    // primeiro caso de borda, e este teste é o que avisa.
    var comparacoes = 0;
    for (User quem : List.of(ana, bruno)) {
      for (Permission permissao : List.of(LER, ESCREVER)) {
        for (Arquivo alvo : List.of(dela, dele)) {
          var exp = iam.motor().explicar(quem, permissao, alvo);
          var direto = iam.motor().avaliar(quem, permissao, alvo);
          assertEquals(direto.getTipo(), exp.getDecisao().getTipo());
          assertEquals(direto.permitido(), exp.getDecisao().permitido());
          assertEquals(direto.getOrigem(), exp.getDecisao().getOrigem());
          comparacoes++;
        }
      }
    }
    assertEquals(8, comparacoes);
  }
}
