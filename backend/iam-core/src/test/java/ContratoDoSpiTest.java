import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;

/**
 * O pacote {@code poo.iam.spi} é a promessa de que o contrato de integração
 * cabe numa página: quem for adaptar o núcleo a outro domínio lê aquele pacote
 * e sabe exatamente o que precisa escrever.
 *
 * Uma promessa dessas se perde por acúmulo — uma classe utilitária aqui, um
 * import do pacote de consultas ali — e ninguém percebe até a página virar
 * três. Este teste é o que a mantém.
 */
class ContratoDoSpiTest {

  private static final Path SPI = Path.of("src/main/java/poo/iam/spi");

  @Test
  void oSpiTemSoInterfaces() throws IOException {
    for (Path arquivo : arquivos()) {
      var texto = Files.readString(arquivo);
      var declaracao = texto.lines()
          .filter(l -> l.startsWith("public ") || l.startsWith("class ") || l.startsWith("enum "))
          .findFirst()
          .orElse("");
      assertTrue(declaracao.contains("interface"),
          arquivo.getFileName() + " não é interface: uma implementação no spi é o núcleo "
              + "decidindo por quem o usa — " + declaracao.trim());
    }
  }

  @Test
  void oSpiNaoPuxaNadaDeDentroDoNucleo() throws IOException {
    for (Path arquivo : arquivos()) {
      for (String linha : Files.readAllLines(arquivo)) {
        if (!linha.startsWith("import "))
          continue;
        var importado = linha.substring("import ".length()).replace(";", "").trim();

        if (importado.startsWith("java."))
          continue;
        // o vocabulário do núcleo, e só ele: Resource, User, Permission...
        if (importado.startsWith("poo.iam.") && importado.lastIndexOf('.') == "poo.iam".length())
          continue;

        fail(arquivo.getFileName() + " importa " + importado + ". O spi só pode falar o "
            + "vocabulário de poo.iam — puxar as consultas, as condições ou o documento "
            + "para cá faz o contrato de integração deixar de caber numa página.");
      }
    }
  }

  private static List<Path> arquivos() throws IOException {
    try (Stream<Path> arquivos = Files.list(SPI)) {
      var lista = arquivos
          .filter(p -> p.toString().endsWith(".java"))
          .filter(p -> !p.getFileName().toString().equals("package-info.java"))
          .toList();
      assertTrue(lista.size() >= 3, "o spi sumiu? achei " + lista.size() + " arquivos");
      return lista;
    }
  }
}
