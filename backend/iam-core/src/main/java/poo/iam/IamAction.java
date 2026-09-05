package poo.iam;

/**
 * As ações sobre o próprio IAM.
 *
 * O vocabulário de ações é aberto e pertence à aplicação — mas assumir um
 * papel é uma operação do núcleo sobre um objeto do núcleo, e por isso a ação
 * mora aqui, como {@code sts:AssumeRole} mora na AWS e não no serviço de quem
 * a chama.
 */
public enum IamAction implements Action {
  ASSUMIR_PAPEL,
}
