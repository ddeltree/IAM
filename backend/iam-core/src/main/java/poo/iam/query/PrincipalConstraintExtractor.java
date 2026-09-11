package poo.iam.query;

import java.util.LinkedHashSet;

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
 * Avalia a condição com o recurso fixado e o principal em aberto, e devolve
 * quem ela ainda poderia alcançar.
 *
 * É a leitura ao contrário que as condições como dado tornaram possível: em
 * {@code turma:professorId = ${principal:id}}, com a turma conhecida, o lado
 * esquerdo vira um valor e a cláusula passa a dizer literalmente qual principal
 * ela atende — sem varrer usuário nenhum.
 *
 * Na dúvida devolve {@link PrincipalConstraint#todos()}: o resultado é um
 * superconjunto, e quem decide de verdade continua sendo o motor.
 */
public final class PrincipalConstraintExtractor implements ConditionVisitor<PrincipalConstraint> {

  private static final String VAR_PRINCIPAL_ID = "${principal:id}";

  /** Contexto montado só com o recurso; as chaves de principal ficam vazias. */
  private final RequestContext doRecurso;

  public PrincipalConstraintExtractor(RequestContext doRecurso) {
    this.doRecurso = doRecurso;
  }

  public static PrincipalConstraint extrair(Condition condicao, RequestContext doRecurso) {
    return condicao.accept(new PrincipalConstraintExtractor(doRecurso));
  }

  @Override
  public PrincipalConstraint visitarSempre(Sempre sempre) {
    return PrincipalConstraint.todos();
  }

  @Override
  public PrincipalConstraint visitarNunca(poo.iam.condition.Nunca nunca) {
    return PrincipalConstraint.ninguem();
  }

  @Override
  public PrincipalConstraint visitarComparacao(Comparacao comparacao) {
    if (dizQuemE(comparacao)) {
      // a chave é do recurso e já tem valor: são exatamente esses principais
      var valores = doRecurso.get(comparacao.getChave());
      return valores.isEmpty()
          ? PrincipalConstraint.ninguem()
          : PrincipalConstraint.apenas(new LinkedHashSet<>(valores));
    }

    // uma variável que só o principal resolve deixa a comparação indecidível aqui:
    // avaliá-la assim mesmo compararia com o texto "${principal:id}" e devolveria
    // ninguém — uma resposta, e errada, onde a certa é "não sei"
    if (dependeDoPrincipal(comparacao))
      return PrincipalConstraint.todos();

    // não fala do principal: ou já dá para decidir pelo recurso, ou não sei
    if (comparacao.getChave().startsWith("principal:"))
      return PrincipalConstraint.todos();
    return comparacao.avaliar(doRecurso)
        ? PrincipalConstraint.todos()
        : PrincipalConstraint.ninguem();
  }

  /**
   * A comparação diz, sozinha, <em>quais</em> principais ela atende?
   *
   * Só a igualdade diz. {@code recurso:dono = ${principal:id}} atende quem tem o id que
   * está no recurso; {@code recurso:dono != ${principal:id}} atende exatamente o
   * contrário, e {@code Parecido}, {@code Maior} e os demais atendem conjuntos que a
   * chave do recurso não enumera. Ler só os valores e ignorar o operador devolvia o
   * conjunto <b>invertido</b> nesses casos — a poda decidindo, em vez de escolher
   * candidatos.
   *
   * O valor também precisa ser só a variável: em
   * {@code ["${principal:id}", "admin"]} o conjunto verdadeiro é maior que o extraído, e
   * um superconjunto é obrigatório — o subconjunto esconderia quem pode.
   */
  private static boolean dizQuemE(Comparacao comparacao) {
    var operador = comparacao.getOperador().name();
    var eIgualdade = operador.equals(Operadores.IGUAL.name())
        || operador.equals(Operadores.PARA_ALGUM_VALOR + Operadores.IGUAL.name());
    return eIgualdade
        && comparacao.getValores().equals(java.util.List.of(VAR_PRINCIPAL_ID))
        && !comparacao.getChave().startsWith("principal:");
  }

  /** Algum valor é uma variável que o contexto sem principal não resolve. */
  private boolean dependeDoPrincipal(Comparacao comparacao) {
    for (String valor : comparacao.getValores()) {
      if (!valor.startsWith("${") || !valor.endsWith("}"))
        continue;
      if (doRecurso.get(valor.substring(2, valor.length() - 1)).size() != 1)
        return true;
    }
    return false;
  }

  @Override
  public PrincipalConstraint visitarTodasAs(TodasAs todas) {
    var res = PrincipalConstraint.todos();
    for (Condition c : todas.getCondicoes())
      res = res.e(c.accept(this));
    return res;
  }

  @Override
  public PrincipalConstraint visitarAlgumaDas(AlgumaDas alguma) {
    var res = PrincipalConstraint.ninguem();
    for (Condition c : alguma.getCondicoes())
      res = res.ou(c.accept(this));
    return res;
  }

  @Override
  public PrincipalConstraint visitarNegacao(Negacao negacao) {
    // negar um conjunto conhecido não dá um conjunto conhecido
    return PrincipalConstraint.todos();
  }

  @Override
  public PrincipalConstraint visitarOpaca(CondicaoOpaca opaca) {
    return PrincipalConstraint.todos();
  }
}
