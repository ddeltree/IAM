package poo.iam;

/**
 * Sobre o que se age. Assim como {@link Action}, é vocabulário aberto: o núcleo
 * não conhece TURMA nem POST — quem os declara é a aplicação.
 */
public interface ResourceType {
  String name();
}
