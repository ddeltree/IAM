package poo.iam;

/** Qualquer objeto que possa ser alvo de uma permissão. */
public interface Resource {
  ResourceType getType();

  /**
   * Identifica a instância. Junto com o tipo forma a referência
   * {@code TIPO/id} — uma simplificação do ARN da AWS, sem partição, serviço
   * nem região, que aqui seriam infraestrutura sem uso.
   */
  String getId();

  /**
   * O recurso que contém este — um post pertence a uma turma, um comentário a
   * uma publicação. É por esta corrente que uma condição sobre "o professor
   * responsável" alcança a turma partindo de um comentário, sem que ninguém
   * precise escrever essa navegação à mão.
   */
  default Resource getPai() {
    return null;
  }
}
