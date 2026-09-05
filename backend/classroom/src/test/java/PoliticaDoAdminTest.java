import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import java.util.TreeSet;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import poo.Main;
import poo.classroom.Turma;
import poo.classroom.iam.ClassroomPermission;
import poo.classroom.iam.ClassroomPermission.Escopo;
import poo.classroom.iam.SecurityContext;
import poo.iam.User;

/**
 * A política do administrador, por extenso.
 *
 * Ela é escrita com curinga — "pode tudo, menos criar conteúdo e menos mexer em
 * matrícula" — e essa é justamente a razão de existir este teste. Curinga diz a
 * intenção em três linhas, mas a consequência de cada uma se espalha por todas
 * as ações do catálogo, inclusive as que ainda não existem. Uma ação nova
 * chamada {@code EXCLUIR_INSTITUICAO} passaria a ser permitida ao
 * administrador sem que ninguém escrevesse nada — e é melhor esse fato
 * aparecer aqui, como uma linha que muda, do que em produção.
 */
class PoliticaDoAdminTest {

  @BeforeEach
  void limpar() {
    Main.resetState();
  }

  @Test
  void oAdministradorModeraTudoMasNaoCriaConteudoNemMatricula() {
    var auth = SecurityContext.getInstance();
    var turma = new Turma("Cálculo", new User("Professor"));

    var pode = new TreeSet<String>();
    var naoPode = new TreeSet<String>();
    for (var permissao : ClassroomPermission.values()) {
      var alvo = permissao.getEscopo() == Escopo.GLOBAL ? null
          : permissao.getResourceType().name().equals("TURMA") ? turma : null;
      (permissao.isAllowed(auth.getAdmin(), alvo) ? pode : naoPode).add(permissao.name());
    }

    assertEquals(new TreeSet<>(List.of(
        "CRIAR_ALUNO",
        "CRIAR_ATIVIDADE",
        "CRIAR_COMENTARIO",
        "CRIAR_POST",
        "CRIAR_TURMA",
        "DESMATRICULAR_ALUNO",
        "MATRICULAR_ALUNO")),
        naoPode,
        "o que o administrador não pode é o que as três negações explícitas dizem");

    // e tudo o mais é dele, sem restrição de turma — inclusive CRIAR_PROFESSOR,
    // que escapa do DENY de CRIAR_* por ser sobre USUARIO, e não sobre TURMA
    assertEquals(ClassroomPermission.values().length - naoPode.size(), pode.size());
    assertEquals(true, pode.contains("CRIAR_PROFESSOR"));
  }
}
