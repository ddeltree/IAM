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

/** Aplica o filtro sobre objetos em memória. */
public final class PredicateRenderer implements ConstraintVisitor<Predicate<Resource>> {

  public static Predicate<Resource> render(ResourceConstraint constraint) {
    return constraint.accept(new PredicateRenderer());
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

  private static java.util.List<String> valores(Resource recurso, String chave) {
    return ContextResolver.padrao().resolver(null, recurso).get(chave);
  }
}
