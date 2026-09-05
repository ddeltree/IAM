package poo.iam.query;

import java.util.LinkedHashSet;

import poo.iam.RequestContext;
import poo.iam.condition.AlgumaDas;
import poo.iam.condition.Comparacao;
import poo.iam.condition.CondicaoOpaca;
import poo.iam.condition.Condition;
import poo.iam.condition.ConditionVisitor;
import poo.iam.condition.Negacao;
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
    var procuraOPrincipal = comparacao.getValores().contains(VAR_PRINCIPAL_ID);

    if (procuraOPrincipal) {
      // a chave é do recurso e já tem valor: são exatamente esses principais
      var valores = doRecurso.get(comparacao.getChave());
      return valores.isEmpty()
          ? PrincipalConstraint.ninguem()
          : PrincipalConstraint.apenas(new LinkedHashSet<>(valores));
    }

    // não fala do principal: ou já dá para decidir pelo recurso, ou não sei
    if (comparacao.getChave().startsWith("principal:"))
      return PrincipalConstraint.todos();
    return comparacao.avaliar(doRecurso)
        ? PrincipalConstraint.todos()
        : PrincipalConstraint.ninguem();
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
