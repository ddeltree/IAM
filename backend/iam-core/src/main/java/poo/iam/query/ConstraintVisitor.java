package poo.iam.query;

import poo.iam.query.ResourceConstraint.Alguma;
import poo.iam.query.ResourceConstraint.AtributoCompara;
import poo.iam.query.ResourceConstraint.AtributoContem;
import poo.iam.query.ResourceConstraint.AtributoIgual;
import poo.iam.query.ResourceConstraint.IdEm;
import poo.iam.query.ResourceConstraint.Nada;
import poo.iam.query.ResourceConstraint.Todas;
import poo.iam.query.ResourceConstraint.Tudo;

/**
 * Percorre o filtro. É aqui que mora o ponto do desenho: o mesmo objeto vira um
 * predicado em memória com um visitante e uma cláusula {@code WHERE} com outro.
 */
public interface ConstraintVisitor<R> {

  R visitarTudo(Tudo tudo);

  R visitarNada(Nada nada);

  R visitarIgual(AtributoIgual igual);

  R visitarContem(AtributoContem contem);

  R visitarCompara(AtributoCompara compara);

  R visitarIdEm(IdEm idEm);

  R visitarTodas(Todas todas);

  R visitarAlguma(Alguma alguma);
}
