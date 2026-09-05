import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import poo.iam.AccessResolver;
import poo.iam.Action;
import poo.iam.ContextResolver;
import poo.iam.Group;
import poo.iam.MembershipManager;
import poo.iam.Permission;
import poo.iam.Resource;
import poo.iam.ResourceType;
import poo.iam.User;
import poo.iam.condition.Condition;
import poo.iam.document.PolicyDocument;
import poo.iam.spi.AttributeProvider;

/**
 * O núcleo decidindo sobre um domínio que não é o classroom.
 *
 * Enquanto os únicos testes do sistema falavam de turma e post, "o núcleo é
 * genérico" era afirmação, não fato verificado: nada garantia que ele não
 * dependesse, em algum canto, de um jeito particular daquele domínio. Aqui há
 * pastas e arquivos, um vocabulário que o núcleo nunca viu, adaptado só pelas
 * interfaces do {@code poo.iam.spi} — e as permissões saem certas.
 *
 * É deliberadamente um domínio pobre: o que se está testando é o núcleo, não a
 * criatividade do exemplo.
 */
class DominioQualquerTest {

  // ---------- o domínio: o que qualquer aplicação escreveria ----------

  enum AcaoDeArquivo implements Action {
    LER, ESCREVER, APAGAR
  }

  enum TipoDeArquivo implements ResourceType {
    PASTA, ARQUIVO
  }

  record Pasta(String id, String donoId) implements Resource {
    public ResourceType getType() {
      return TipoDeArquivo.PASTA;
    }

    public String getId() {
      return id;
    }
  }

  record Arquivo(String id, String autorId, Pasta pasta) implements Resource {
    public ResourceType getType() {
      return TipoDeArquivo.ARQUIVO;
    }

    public String getId() {
      return id;
    }

    @Override
    public Resource getPai() {
      return pasta;
    }
  }

  /** A adaptação inteira: como ler os atributos de cada tipo. */
  static void registrarAtributos(ContextResolver resolver) {
    resolver.registrar(new AttributeProvider() {
      public ResourceType tipo() {
        return TipoDeArquivo.PASTA;
      }

      public Map<String, List<String>> atributosDe(Resource r) {
        return Map.of("donoId", List.of(((Pasta) r).donoId()));
      }
    });
    resolver.registrar(new AttributeProvider() {
      public ResourceType tipo() {
        return TipoDeArquivo.ARQUIVO;
      }

      public Map<String, List<String>> atributosDe(Resource r) {
        return Map.of("autorId", List.of(((Arquivo) r).autorId()));
      }
    });
  }

  // ---------- a política ----------

  static final Permission LER_ARQUIVO = new Permission(AcaoDeArquivo.LER, TipoDeArquivo.ARQUIVO);
  static final Permission APAGAR_ARQUIVO =
      new Permission(AcaoDeArquivo.APAGAR, TipoDeArquivo.ARQUIVO);

  /** O autor mexe no que é dele; o dono da pasta manda em tudo que está nela. */
  static final Condition AUTOR = Condition.igual("recurso:autorId", "${principal:id}");
  static final Condition DONO_DA_PASTA = Condition.igual("pasta:donoId", "${principal:id}");

  private User ana;
  private User bruno;
  private Group colaboradores;
  private Pasta pasta;
  private Arquivo deAna;

  @BeforeEach
  void montar() {
    ContextResolver.padrao().limpar();
    registrarAtributos(ContextResolver.padrao());

    ana = new User("Ana");
    bruno = new User("Bruno");
    colaboradores = new Group("Colaboradores");
    MembershipManager.link(ana, colaboradores);
    MembershipManager.link(bruno, colaboradores);

    colaboradores.grantPermission(LER_ARQUIVO, AUTOR);
    colaboradores.grantPermission(APAGAR_ARQUIVO, AUTOR.ou(DONO_DA_PASTA));

    pasta = new Pasta("p1", bruno.getId());
    deAna = new Arquivo("a1", ana.getId(), pasta);
  }

  @Test
  void oAutorLeOProprioArquivo() {
    assertTrue(AccessResolver.isAllowed(ana, LER_ARQUIVO, deAna));
    assertFalse(AccessResolver.isAllowed(bruno, LER_ARQUIVO, deAna),
        "Bruno não é o autor, e a permissão de ler não olha a pasta");
  }

  @Test
  void oDonoDaPastaApagaOQueEstaNela() {
    // a condição fala da pasta, mas o recurso avaliado é o arquivo: quem
    // alcança um pelo outro é a corrente de getPai(), sem ninguém navegar à mão
    assertTrue(AccessResolver.isAllowed(bruno, APAGAR_ARQUIVO, deAna));
    assertTrue(AccessResolver.isAllowed(ana, APAGAR_ARQUIVO, deAna), "Ana é a autora");
  }

  @Test
  void umaPermissaoNaoAlcancaORecursoErrado() {
    assertFalse(AccessResolver.isAllowed(bruno, APAGAR_ARQUIVO, pasta),
        "APAGAR_ARQUIVO é sobre ARQUIVO; recebendo uma PASTA, não se aplica");
  }

  @Test
  void aPoliticaDesteDominioTambemSeLeComoDocumento() {
    var doc = PolicyDocument.deGrupo(colaboradores);

    assertEquals("Colaboradores", doc.get("principal"));
    @SuppressWarnings("unchecked")
    var statements = (List<Map<String, Object>>) doc.get("statements");
    assertEquals(2, statements.size());

    var apagar = statements.stream()
        .filter(s -> "APAGAR".equals(s.get("action")))
        .findFirst()
        .orElseThrow();
    assertEquals("ARQUIVO", apagar.get("resourceType"));
    // a condição saiu como dado, com o vocabulário deste domínio dentro
    assertTrue(apagar.get("condition").toString().contains("pasta:donoId"),
        "a condição não apareceu no documento: " + apagar);
  }
}
