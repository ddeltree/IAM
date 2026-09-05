import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import poo.iam.Action;
import poo.iam.IamFactory;
import poo.iam.Permission;
import poo.iam.PrincipalResource;
import poo.iam.Statement;
import poo.iam.User;
import poo.iam.condition.Condition;
import poo.iam.condition.OperatorRegistry;
import poo.iam.document.ConditionDocument;

/**
 * Os operadores além da igualdade, e as chaves que só existem em tempo de
 * requisição.
 *
 * Enquanto o vocabulário era só "igual", "parecido" e "nulo", uma regra como
 * "só durante a aula" não tinha como ser escrita — e a alternativa seria
 * escrevê-la em Java, que é exatamente o que faz uma política deixar de ser
 * consultável.
 */
class OperadoresTest {

  enum Acao implements Action {
    ENTRAR
  }

  static final Permission ENTRAR = new Permission(Acao.ENTRAR, PrincipalResource.USUARIO);

  /** Relógio parado: uma política com horário é intestável sem isto. */
  private static final Clock DEZ_DA_MANHA =
      Clock.fixed(Instant.parse("2026-09-04T10:00:00Z"), ZoneOffset.UTC);

  private static poo.iam.Iam iamAs(Clock relogio) {
    var iam = IamFactory.novo().construir();
    iam.contexto().comRelogio(relogio);
    return iam;
  }

  @Test
  void soDuranteAAula() {
    var iam = iamAs(DEZ_DA_MANHA);
    var aluno = new User("aluno");
    aluno.add(Statement.de(poo.iam.Effect.ALLOW, "ENTRAR", "*",
        Condition.todasAs(
            Condition.depoisDe("contexto:hora", "08:00"),
            Condition.antesDe("contexto:hora", "12:00"))));

    assertTrue(iam.motor().isAllowed(aluno, ENTRAR, null), "10h está na janela");

    // o mesmo usuário, a mesma política, outro horário
    var deNoite = iamAs(Clock.fixed(Instant.parse("2026-09-04T22:00:00Z"), ZoneOffset.UTC));
    assertFalse(deNoite.motor().isAllowed(aluno, ENTRAR, null), "22h está fora");
  }

  @Test
  void numerosComparamComoNumerosENaoComoTexto() {
    var iam = iamAs(DEZ_DA_MANHA);
    var user = new User("qualquer");
    user.add(Statement.de(poo.iam.Effect.ALLOW, "ENTRAR", "*",
        Condition.maiorQue("requisicao:nota", "9")));

    // como texto, "10" < "9"; o operador numérico não cai nessa
    assertTrue(iam.motor().isAllowed(user, ENTRAR, null, Map.of("nota", List.of("10"))));
    assertFalse(iam.motor().isAllowed(user, ENTRAR, null, Map.of("nota", List.of("8"))));
    // valor que não é número não vira palpite: é falso
    assertFalse(iam.motor().isAllowed(user, ENTRAR, null, Map.of("nota", List.of("ótimo"))));
  }

  @Test
  void seExistirDeixaPassarQuandoAChaveNaoVeio() {
    var operadores = OperatorRegistry.padrao();
    var comPrefixo = operadores.get("SeExistir:Igual");

    assertTrue(comPrefixo.testar(List.of(), List.of("sim")), "chave ausente passa");
    assertTrue(comPrefixo.testar(List.of("sim"), List.of("sim")));
    assertFalse(comPrefixo.testar(List.of("nao"), List.of("sim")));
  }

  @Test
  void osPrefixosSeCompoem() {
    // é o que evita um operador novo para cada combinação, como na AWS
    var operador = OperatorRegistry.padrao().get("SeExistir:ParaAlgumValor:Igual");
    assertEquals("SeExistir:ParaAlgumValor:Igual", operador.name());
    assertTrue(operador.testar(List.of("a", "b"), List.of("b")));
    assertFalse(operador.testar(List.of("a", "b"), List.of("c")));
    assertTrue(operador.testar(List.of(), List.of("c")));
  }

  @Test
  void osOperadoresNovosVoltamDoDocumento() {
    var original = Condition.todasAs(
        Condition.depoisDe("contexto:hora", "08:00"),
        Condition.maiorQue("requisicao:nota", "9"));

    var relido = ConditionDocument.ler(ConditionDocument.escrever(original));
    assertEquals(original, relido,
        "um operador que não volta do documento torna a política impossível de guardar");
  }

  @Test
  void nuncaDesligaAClausulaSemApagaLa() {
    var iam = iamAs(DEZ_DA_MANHA);
    var user = new User("qualquer");
    user.add(Statement.de(poo.iam.Effect.ALLOW, "ENTRAR", "*", Condition.NUNCA));

    assertFalse(iam.motor().isAllowed(user, ENTRAR, null));
    // mas ela continua lá, e continua aparecendo no documento
    assertEquals(1, user.getStatements().size());
    assertEquals(Condition.NUNCA, ConditionDocument.ler(
        ConditionDocument.escrever(Condition.NUNCA)));
  }
}
