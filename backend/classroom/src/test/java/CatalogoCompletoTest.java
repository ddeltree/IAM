import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.TreeSet;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;

import poo.classroom.iam.ClassroomAction;
import poo.classroom.iam.ClassroomPermission;

/**
 * O catálogo precisa estar completo, e nada no compilador exige isso.
 *
 * Uma ação que os controllers pedem mas que não está no catálogo continua
 * sendo concedida e decidida normalmente — ela só some das respostas de "o que
 * posso fazer aqui". O sintoma é a interface escondendo um botão que
 * funcionaria: nenhum erro, nenhum 403, só uma funcionalidade invisível.
 *
 * Ficou pior desde que a política aceita curinga. Antes, uma ação fora do
 * catálogo provavelmente também estaria fora da política, e o efeito seria
 * coerente. Agora {@code allow("*", "*")} concede tudo o que existe, inclusive
 * o que ninguém catalogou.
 */
class CatalogoCompletoTest {

  @Test
  void todaAcaoDeclaradaEstaNoCatalogo() {
    var catalogadas = new TreeSet<String>();
    for (var permissao : ClassroomPermission.values())
      catalogadas.add(permissao.get().getAction().name());

    var declaradas = new TreeSet<String>();
    for (var acao : ClassroomAction.values())
      declaradas.add(acao.name());

    assertEquals(declaradas, catalogadas,
        "toda ação declarada precisa de uma entrada no catálogo, senão ela existe "
            + "para o motor e não existe para quem pergunta o que pode fazer");
  }

  @Test
  void asPermissoesSaoDeclaradasEmUmLugarSo() throws IOException {
    // o teste acima só vale enquanto ClassroomPermission for a única fonte:
    // um `new Permission(...)` solto num controller escaparia dele
    try (Stream<Path> arquivos = Files.walk(Path.of("src/main/java"))) {
      for (Path arquivo : arquivos.filter(p -> p.toString().endsWith(".java")).toList()) {
        if (arquivo.endsWith("ClassroomPermission.java"))
          continue;
        assertTrue(!Files.readString(arquivo).contains("new Permission("),
            arquivo + " constrói uma Permission fora do catálogo — ela decidiria "
                + "normalmente e não apareceria em GET /permissoes");
      }
    }
  }

  @Test
  void oCatalogoEntregueAoNucleoEOMesmo() {
    assertEquals(ClassroomPermission.values().length, ClassroomPermission.CATALOGO.todas().size());
    assertEquals(
        Arrays.stream(ClassroomPermission.values())
            .filter(p -> p.getEscopo() == ClassroomPermission.Escopo.GLOBAL).count(),
        ClassroomPermission.CATALOGO.semAlvo().size());
  }
}
