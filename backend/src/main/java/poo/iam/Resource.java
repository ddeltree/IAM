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
}
