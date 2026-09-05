import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTimeoutPreemptively;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.Test;

import poo.iam.AccessResolver;
import poo.iam.Action;
import poo.iam.ContextResolver;
import poo.iam.Permission;
import poo.iam.Principal;
import poo.iam.PrincipalResource;
import poo.iam.ResourceType;
import poo.iam.Statement;
import poo.iam.User;

/**
 * A política de um principal é a dele mais a de quem ele herda, e o motor
 * descobre isso percorrendo um grafo — não uma lista de grupos.
 *
 * Grafo aceita ciclo. Com {@link poo.iam.User} e {@link poo.iam.Group} ele não
 * acontece, porque grupo não herda de ninguém; mas assim que uma aplicação
 * implementa {@link Principal} sobre a hierarquia dela — e é para isso que a
 * interface existe —, dois principais herdando um do outro deixam de ser
 * hipótese. Sem conjunto de visitados isso é recursão infinita: o servidor não
 * responde errado, ele para de responder.
 */
class GrafoDePrincipaisTest {

  enum Acao implements Action {
    ENTRAR, SAIR
  }

  static final Permission ENTRAR = new Permission(Acao.ENTRAR, PrincipalResource.USUARIO);
  static final Permission SAIR = new Permission(Acao.SAIR, PrincipalResource.USUARIO);

  /** Um principal da aplicação, que não é o User do núcleo. */
  static final class Qualquer implements Principal {
    private final String nome;
    private final Set<Statement> statements = new LinkedHashSet<>();
    private final List<Principal> herda = new ArrayList<>();

    Qualquer(String nome) {
      this.nome = nome;
    }

    Qualquer concede(Permission p) {
      statements.add(Statement.allow(p));
      return this;
    }

    Qualquer herdaDe(Principal outro) {
      herda.add(outro);
      return this;
    }

    public String getId() {
      return nome;
    }

    public String getName() {
      return nome;
    }

    public Set<Statement> getStatements() {
      return statements;
    }

    @Override
    public Collection<Principal> herdaDe() {
      return herda;
    }
  }

  private static final Duration LIMITE = Duration.ofSeconds(2);

  @Test
  void umPrincipalQueHerdaDeSiMesmoNaoTrava() {
    var sozinho = new Qualquer("sozinho").concede(ENTRAR);
    sozinho.herdaDe(sozinho);

    assertTimeoutPreemptively(LIMITE, () -> {
      assertTrue(AccessResolver.isAllowed(sozinho, ENTRAR, null));
      assertFalse(AccessResolver.isAllowed(sozinho, SAIR, null));
    });
  }

  @Test
  void doisPrincipaisEmCicloSomamAsPoliticasSemTravar() {
    var a = new Qualquer("A").concede(ENTRAR);
    var b = new Qualquer("B").concede(SAIR);
    a.herdaDe(b);
    b.herdaDe(a);

    assertTimeoutPreemptively(LIMITE, () -> {
      // cada um alcança a política do outro, e nenhum se perde na volta
      assertTrue(AccessResolver.isAllowed(a, ENTRAR, null));
      assertTrue(AccessResolver.isAllowed(a, SAIR, null));
      assertTrue(AccessResolver.isAllowed(b, ENTRAR, null));
      assertTrue(AccessResolver.isAllowed(b, SAIR, null));
    });
  }

  @Test
  void aDecisaoDizDeQualPrincipalVeioAClausula() {
    ContextResolver.padrao().limpar();
    var diretoria = new Qualquer("Diretoria").concede(SAIR);
    var pessoa = new Qualquer("pessoa").concede(ENTRAR).herdaDe(diretoria);

    // herdada: a decisão nomeia onde a cláusula estava
    assertEquals("Diretoria", AccessResolver.avaliar(pessoa, SAIR, null).getOrigem());
    // própria: "inline", como na AWS
    assertEquals("inline", AccessResolver.avaliar(pessoa, ENTRAR, null).getOrigem());
  }

  @Test
  void quemQuiserAcompanharAPoliticaRegistraUmOuvinte() {
    var anotado = new ArrayList<String>();

    // o núcleo não imprime mais nada por conta própria; isto costumava ser um
    // System.out.println dentro do próprio User
    var user = new User("Ana").comOuvinte(
        (alvo, mudanca, permission) -> anotado.add(alvo.getName() + " " + mudanca + " " + permission));

    user.grantPermission(ENTRAR);
    user.denyPermission(SAIR);
    user.grantPermission(ENTRAR); // repetida: não muda nada, não avisa nada

    assertEquals(List.of("Ana CONCEDIDA ENTRAR:USUARIO", "Ana NEGADA SAIR:USUARIO"), anotado);
  }

  @Test
  void oIdPodeVirDaAplicacao() {
    // o contador do núcleo é conveniência, não imposição: aqui o id é o que a
    // aplicação já usa
    var user = new User("550e8400-e29b-41d4-a716-446655440000", "Ana");
    assertEquals("550e8400-e29b-41d4-a716-446655440000", user.getId());

    var comCondicao = Statement.allow(ENTRAR,
        poo.iam.condition.Condition.igual("recurso:id", "${principal:id}"));
    user.grantPermission(ENTRAR, poo.iam.condition.Condition.igual("recurso:id", "${principal:id}"));
    assertTrue(user.getStatements().contains(comCondicao),
        "a variável de política resolve contra o id que a aplicação deu");
  }
}
