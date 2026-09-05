package poo.iam;

import java.util.Objects;

/**
 * Os recursos que uma cláusula alcança, escritos como {@code TIPO/id} — o campo
 * {@code Resource} da AWS, onde se escreve
 * {@code arn:aws:s3:::relatorios/2024/*}.
 *
 * Os dois lados aceitam curinga: {@code TURMA/3} é uma turma, {@code TURMA/*}
 * são todas, {@code *} é qualquer coisa. É a simplificação do ARN que este
 * sistema já usava nas referências da API — sem partição, serviço nem região,
 * que aqui seriam infraestrutura sem uso.
 *
 * <h2>Por que não bastava a condição</h2>
 *
 * Mirar um recurso específico já era possível, escrevendo
 * {@code Igual {"recurso:id": "3"}}. Mas aí "sobre o que esta cláusula fala" só
 * se descobre avaliando a condição, e as consultas precisam saber isso
 * <em>antes</em> — é o que permite não considerar sequer uma cláusula sobre
 * {@code TURMA/3} quando a pergunta é sobre a turma 9. O elemento existe tanto
 * para dizer o que se quer dizer quanto para a poda ter o que ler.
 */
public final class ResourcePattern {

  public static final ResourcePattern TUDO = new ResourcePattern("*", "*");

  private final String tipo;
  private final String id;

  private ResourcePattern(String tipo, String id) {
    this.tipo = tipo;
    this.id = id;
  }

  /** Aceita {@code "*"}, {@code "TURMA"} (= todas), {@code "TURMA/3"}. */
  public static ResourcePattern de(String referencia) {
    if (referencia == null || referencia.equals("*"))
      return TUDO;
    var barra = referencia.indexOf('/');
    if (barra < 0)
      return new ResourcePattern(referencia, "*");
    return new ResourcePattern(referencia.substring(0, barra), referencia.substring(barra + 1));
  }

  /** Todos os recursos de um tipo. */
  public static ResourcePattern deTipo(ResourceType tipo) {
    return new ResourcePattern(tipo.name(), "*");
  }

  /** Um recurso específico. */
  public static ResourcePattern de(Resource recurso) {
    return new ResourcePattern(recurso.getType().name(), recurso.getId());
  }

  public boolean casaTipo(ResourceType tipo) {
    return Curinga.casa(tipo.name(), this.tipo);
  }

  /**
   * Recurso nulo casa sempre: é como se pedem as ações que não têm alvo, como
   * criar uma turma.
   */
  public boolean casa(Resource recurso) {
    if (recurso == null)
      return true;
    return casaTipo(recurso.getType()) && Curinga.casa(recurso.getId(), id);
  }

  /** O padrão fixa um id, em vez de aceitar qualquer um? */
  public boolean idExato() {
    return id.indexOf('*') < 0 && id.indexOf('?') < 0;
  }

  public String getTipo() {
    return tipo;
  }

  public String getId() {
    return id;
  }

  @Override
  public boolean equals(Object o) {
    if (!(o instanceof ResourcePattern))
      return false;
    var that = (ResourcePattern) o;
    return tipo.equals(that.tipo) && id.equals(that.id);
  }

  @Override
  public int hashCode() {
    return Objects.hash(tipo, id);
  }

  /**
   * {@code *}, {@code TURMA} ou {@code TURMA/3}.
   *
   * O tipo sozinho já significa "todas as instâncias" — é assim que
   * {@link #de(String)} o interpreta —, então escrever {@code TURMA/*} seria
   * dizer a mesma coisa com mais ruído.
   */
  @Override
  public String toString() {
    if (tipo.equals("*") && id.equals("*"))
      return "*";
    return id.equals("*") ? tipo : tipo + "/" + id;
  }
}
