package poo.iam;

/** Qualquer objeto que possa ser alvo de uma permissão. */
public interface Resource {
  ResourceType getType();
}
