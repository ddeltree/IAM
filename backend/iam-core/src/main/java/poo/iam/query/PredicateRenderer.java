package poo.iam.query;

import java.util.function.Predicate;

import poo.iam.ContextResolver;
import poo.iam.Resource;
import poo.iam.query.ResourceConstraint.Alguma;
import poo.iam.query.ResourceConstraint.AtributoContem;
import poo.iam.query.ResourceConstraint.AtributoIgual;
import poo.iam.query.ResourceConstraint.Nada;
import poo.iam.query.ResourceConstraint.Todas;
import poo.iam.query.ResourceConstraint.Tudo;

/**
 * Aplica o filtro sobre objetos em memória.
 *
 * O par deste é o {@link SqlWhereRenderer}: os dois percorrem a mesma
 * {@link ResourceConstraint} e a escrevem em lugares diferentes. Este precisa
 * do {@link ContextResolver} porque ler o atributo de um objeto é justamente o
 * que só a aplicação sabe fazer.
 */
public final class PredicateRenderer implements ConstraintVisitor<Predicate<Resource>> {

  private final ContextResolver contexto;

  public PredicateRenderer(ContextResolver contexto) {
    this.contexto = contexto;
  }

  public static Predicate<Resource> render(ResourceConstraint constraint, ContextResolver contexto) {
    return constraint.accept(new PredicateRenderer(contexto));
  }

  @Override
  public Predicate<Resource> visitarTudo(Tudo tudo) {
    return recurso -> true;
  }

  @Override
  public Predicate<Resource> visitarNada(Nada nada) {
    return recurso -> false;
  }

  @Override
  public Predicate<Resource> visitarIgual(AtributoIgual igual) {
    return recurso -> valores(recurso, igual.getChave()).contains(igual.getValor());
  }

  @Override
  public Predicate<Resource> visitarContem(AtributoContem contem) {
    return recurso -> valores(recurso, contem.getChave()).contains(contem.getValor());
  }

  @Override
  public Predicate<Resource> visitarTodas(Todas todas) {
    return recurso -> todas.getPartes().stream().allMatch(p -> p.accept(this).test(recurso));
  }

  @Override
  public Predicate<Resource> visitarAlguma(Alguma alguma) {
    return recurso -> alguma.getPartes().stream().anyMatch(p -> p.accept(this).test(recurso));
  }

  private java.util.List<String> valores(Resource recurso, String chave) {
    return contexto.resolver(null, recurso).get(chave);
  }
}
