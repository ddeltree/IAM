package poo.iam;

/**
 * Os recursos que o próprio núcleo define. Usuários e grupos são gerenciados
 * pelo IAM, então são recursos dele — como o {@code arn:aws:iam::…:user/bob} da
 * AWS.
 */
public enum PrincipalResource implements ResourceType {
  USUARIO,
  GRUPO,
}
