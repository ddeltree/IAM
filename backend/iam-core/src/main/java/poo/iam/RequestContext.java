package poo.iam;

import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * Os dados sobre os quais uma condição decide — o <em>request context</em> da
 * AWS, onde vivem chaves como {@code aws:username} ou {@code s3:prefix}.
 *
 * Os valores são sempre listas de texto: uma chave pode ser multivalorada
 * ({@code turma:alunoIds}) e os operadores convertem do texto quando precisam.
 * Guardar tudo como texto é o que permite a condição ser dado serializável em
 * vez de código.
 */
public final class RequestContext {

  private final Principal principal;
  private final Resource recurso;
  private final Map<String, List<String>> valores;

  RequestContext(Principal principal, Resource recurso, Map<String, List<String>> valores) {
    this.principal = principal;
    this.recurso = recurso;
    this.valores = Collections.unmodifiableMap(valores);
  }

  public List<String> get(String chave) {
    return valores.getOrDefault(chave, List.of());
  }

  public boolean has(String chave) {
    return valores.containsKey(chave) && !valores.get(chave).isEmpty();
  }

  public Map<String, List<String>> getValores() {
    return valores;
  }

  public Principal getPrincipal() {
    return principal;
  }

  public Resource getRecurso() {
    return recurso;
  }

}
