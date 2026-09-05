package poo.iam.query;

import java.util.ArrayList;

import poo.iam.RequestContext;
import poo.iam.condition.AlgumaDas;
import poo.iam.condition.Comparacao;
import poo.iam.condition.CondicaoOpaca;
import poo.iam.condition.Condition;
import poo.iam.condition.ConditionVisitor;
import poo.iam.condition.Negacao;
import poo.iam.condition.Operadores;
import poo.iam.condition.Sempre;
import poo.iam.condition.TodasAs;

/**
 * O dual do {@link PrincipalConstraintExtractor}: fixa o principal e deixa o
 * recurso em aberto.
 *
 * O que sobra é um filtro sobre atributos do recurso — que é exatamente a forma
 * de uma cláusula {@code WHERE}. Na dúvida devolve {@code Tudo}, o que só faz a
 * consulta considerar mais candidatos do que o necessário.
 */
public final class ResourceConstraintExtractor implements ConditionVisitor<ResourceConstraint> {

  private final RequestContext doPrincipal;

  public ResourceConstraintExtractor(RequestContext doPrincipal) {
    this.doPrincipal = doPrincipal;
  }

  public static ResourceConstraint extrair(Condition condicao, RequestContext doPrincipal) {
    return condicao.accept(new ResourceConstraintExtractor(doPrincipal));
  }

  @Override
  public ResourceConstraint visitarSempre(Sempre sempre) {
    return ResourceConstraint.Tudo.INSTANCIA;
  }

  @Override
  public ResourceConstraint visitarNunca(poo.iam.condition.Nunca nunca) {
    return ResourceConstraint.Nada.INSTANCIA;
  }

  @Override
  public ResourceConstraint visitarComparacao(Comparacao comparacao) {
    var chave = comparacao.getChave();

    // fala do principal, que já está fixado: dá para decidir agora
    if (chave.startsWith("principal:"))
      return comparacao.avaliar(doPrincipal)
          ? ResourceConstraint.Tudo.INSTANCIA
          : ResourceConstraint.Nada.INSTANCIA;

    var valor = valorFixado(comparacao);
    if (valor == null)
      return ResourceConstraint.Tudo.INSTANCIA;

    var operador = comparacao.getOperador().name();
    if (operador.equals(Operadores.IGUAL.name()))
      return new ResourceConstraint.AtributoIgual(chave, valor);
    if (operador.equals(Operadores.PARA_ALGUM_VALOR + Operadores.IGUAL.name()))
      return new ResourceConstraint.AtributoContem(chave, valor);

    // as comparações de ordem, que sem este ramo virariam Tudo — correto, mas
    // inútil: o banco varreria a tabela para o motor descartar depois
    var simbolo = simboloDe(operador);
    if (simbolo != null)
      return new ResourceConstraint.AtributoCompara(chave, simbolo, valor);

    return ResourceConstraint.Tudo.INSTANCIA;
  }

  /**
   * O símbolo do operador de ordem, ou {@code null} se não for um.
   *
   * Números e datas compartilham os quatro símbolos: quem sabe distinguir
   * "maior que 9" de "depois das 8h" é quem compara os valores, não o filtro.
   */
  private static String simboloDe(String operador) {
    if (operador.equals(Operadores.MAIOR.name()) || operador.equals(Operadores.DATA_DEPOIS.name()))
      return ">";
    if (operador.equals(Operadores.MAIOR_OU_IGUAL.name()))
      return ">=";
    if (operador.equals(Operadores.MENOR.name()) || operador.equals(Operadores.DATA_ANTES.name()))
      return "<";
    if (operador.equals(Operadores.MENOR_OU_IGUAL.name()))
      return "<=";
    return null;
  }

  /** Resolve o valor esperado, expandindo a variável de política. */
  private String valorFixado(Comparacao comparacao) {
    if (comparacao.getValores().size() != 1)
      return null;
    var valor = comparacao.getValores().get(0);
    if (!valor.startsWith("${") || !valor.endsWith("}"))
      return valor;
    var doContexto = doPrincipal.get(valor.substring(2, valor.length() - 1));
    return doContexto.size() == 1 ? doContexto.get(0) : null;
  }

  @Override
  public ResourceConstraint visitarTodasAs(TodasAs todas) {
    var partes = new ArrayList<ResourceConstraint>();
    for (Condition c : todas.getCondicoes()) {
      var parte = c.accept(this);
      if (parte instanceof ResourceConstraint.Nada)
        return ResourceConstraint.Nada.INSTANCIA;
      if (!(parte instanceof ResourceConstraint.Tudo))
        partes.add(parte);
    }
    if (partes.isEmpty())
      return ResourceConstraint.Tudo.INSTANCIA;
    return partes.size() == 1 ? partes.get(0) : new ResourceConstraint.Todas(partes);
  }

  @Override
  public ResourceConstraint visitarAlgumaDas(AlgumaDas alguma) {
    var partes = new ArrayList<ResourceConstraint>();
    for (Condition c : alguma.getCondicoes()) {
      var parte = c.accept(this);
      // basta um lado irrestrito para o todo ser irrestrito
      if (parte instanceof ResourceConstraint.Tudo)
        return ResourceConstraint.Tudo.INSTANCIA;
      if (!(parte instanceof ResourceConstraint.Nada))
        partes.add(parte);
    }
    if (partes.isEmpty())
      return ResourceConstraint.Nada.INSTANCIA;
    return partes.size() == 1 ? partes.get(0) : new ResourceConstraint.Alguma(partes);
  }

  @Override
  public ResourceConstraint visitarNegacao(Negacao negacao) {
    return ResourceConstraint.Tudo.INSTANCIA;
  }

  @Override
  public ResourceConstraint visitarOpaca(CondicaoOpaca opaca) {
    return ResourceConstraint.Tudo.INSTANCIA;
  }
}
