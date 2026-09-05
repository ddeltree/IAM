package poo.iam;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Alguém exercendo um papel, por enquanto.
 *
 * É um {@link Principal} como qualquer outro, e é isso que faz papéis caberem
 * no sistema sem que o motor mude uma linha: a sessão herda do papel, e a
 * travessia do grafo de principais já sabia somar políticas assim.
 *
 * <h2>O que ela não carrega</h2>
 *
 * A sessão não tem política própria. A AWS permite uma <em>session policy</em>,
 * mas lá ela <b>intersecta</b> — só pode restringir o que o papel concede. Este
 * motor soma políticas, então uma política de sessão implementada aqui
 * ampliaria em vez de restringir, que é o oposto do que o nome promete. Melhor
 * não oferecer do que oferecer enganando.
 *
 * <h2>Quem agiu</h2>
 *
 * {@code principal:id} passa a ser o id da sessão, e não o da pessoa — como na
 * AWS, onde {@code aws:userid} vira o id do papel. Quem estava por trás fica em
 * {@code sessao:origem}, e é isso que permite escrever "o moderador age em
 * tudo, menos no que é dele".
 */
public final class Session implements Principal {

  private final String id;
  private final Role papel;
  private final Principal origem;

  Session(String id, Role papel, Principal origem) {
    this.id = id;
    this.papel = papel;
    this.origem = origem;
  }

  @Override
  public String getId() {
    return id;
  }

  @Override
  public String getName() {
    return origem.getName() + " como " + papel.getName();
  }

  /** Vazio de propósito — veja a nota sobre política de sessão. */
  @Override
  public Set<Statement> getStatements() {
    return Set.of();
  }

  @Override
  public List<Principal> herdaDe() {
    return List.of(papel);
  }

  @Override
  public Map<String, List<String>> chavesDeContexto() {
    return Map.of(
        "sessao:papel", List.of(papel.getName()),
        "sessao:origem", List.of(origem.getId()),
        "sessao:origemNome", List.of(String.valueOf(origem.getName())));
  }

  public Role getPapel() {
    return papel;
  }

  /** Quem assumiu o papel. */
  public Principal getOrigem() {
    return origem;
  }

  @Override
  public String toString() {
    return getName();
  }
}
