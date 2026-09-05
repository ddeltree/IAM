package poo.iam.query;

import java.util.stream.Collectors;

import poo.iam.spi.SqlMapping;

import poo.iam.query.ResourceConstraint.Alguma;
import poo.iam.query.ResourceConstraint.AtributoContem;
import poo.iam.query.ResourceConstraint.AtributoIgual;
import poo.iam.query.ResourceConstraint.Nada;
import poo.iam.query.ResourceConstraint.Todas;
import poo.iam.query.ResourceConstraint.Tudo;

/**
 * O mesmo filtro, agora como cláusula {@code WHERE}.
 *
 * Nada aqui conhece a política: o renderizador só percorre o
 * {@link ResourceConstraint} que a extração produziu. Trocar
 * {@link PredicateRenderer} por este move o filtro do laço em memória para
 * dentro do banco sem alterar a política, o motor, nem o controller.
 *
 * Vale a mesma regra de segurança: o SQL escolhe candidatos, o motor decide.
 */
public final class SqlWhereRenderer implements ConstraintVisitor<String> {

  private final SqlMapping mapeamento;

  public SqlWhereRenderer(SqlMapping mapeamento) {
    this.mapeamento = mapeamento;
  }

  public static String render(ResourceConstraint constraint, SqlMapping mapeamento) {
    return constraint.accept(new SqlWhereRenderer(mapeamento));
  }

  @Override
  public String visitarTudo(Tudo tudo) {
    return "1=1";
  }

  @Override
  public String visitarNada(Nada nada) {
    return "1=0";
  }

  @Override
  public String visitarIgual(AtributoIgual igual) {
    var fragmento = mapeamento.igual(igual.getChave(), igual.getValor());
    // chave sem tradução vira "não filtra": mais candidatos, nunca menos
    return fragmento == null ? "1=1" : fragmento;
  }

  @Override
  public String visitarContem(AtributoContem contem) {
    var fragmento = mapeamento.contem(contem.getChave(), contem.getValor());
    return fragmento == null ? "1=1" : fragmento;
  }

  @Override
  public String visitarTodas(Todas todas) {
    return juntar(todas.getPartes().stream().map(p -> p.accept(this)).toList(), " AND ");
  }

  @Override
  public String visitarAlguma(Alguma alguma) {
    return juntar(alguma.getPartes().stream().map(p -> p.accept(this)).toList(), " OR ");
  }

  private static String juntar(java.util.List<String> partes, String separador) {
    return partes.stream().collect(Collectors.joining(separador, "(", ")"));
  }
}
