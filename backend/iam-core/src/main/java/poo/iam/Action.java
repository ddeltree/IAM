package poo.iam;

/**
 * O que se pode fazer. Vocabulário aberto de propósito: cada aplicação declara
 * as próprias ações (normalmente como um enum que implementa esta interface),
 * do mesmo jeito que a AWS usa nomes como {@code "s3:GetObject"} em vez de uma
 * lista fechada dentro do IAM.
 */
public interface Action {
  String name();
}
