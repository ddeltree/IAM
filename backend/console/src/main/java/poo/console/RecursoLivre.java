package poo.console;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import poo.iam.Policy;
import poo.iam.Resource;
import poo.iam.ResourceType;

/**
 * Um recurso cujos atributos são o que você digitou.
 *
 * No classroom, "os atributos de uma turma" é uma decisão de projeto escrita em
 * código. Aqui é um mapa que o usuário preenche — e é isso que permite escrever
 * uma condição sobre qualquer coisa sem antes modelar um domínio.
 *
 * Ele carrega os próprios atributos, e é por isso que o console registra um
 * único {@code AttributeProvider} padrão em vez de um por tipo criado.
 */
public final class RecursoLivre implements Resource {

  private final String tipo;
  private final String id;
  private final Map<String, List<String>> atributos = new LinkedHashMap<>();
  private String paiRef;
  private Policy politica;
  private Cenario cenario;

  public RecursoLivre(String tipo, String id) {
    this.tipo = tipo;
    this.id = id;
  }

  /** A referência {@code TIPO/id}, que é como o console o endereça. */
  public String getRef() {
    return tipo + "/" + id;
  }

  @Override
  public ResourceType getType() {
    return new TipoLivre(tipo);
  }

  @Override
  public String getId() {
    return id;
  }

  public String getTipo() {
    return tipo;
  }

  public Map<String, List<String>> getAtributos() {
    return atributos;
  }

  public void setAtributos(Map<String, List<String>> novos) {
    atributos.clear();
    if (novos != null)
      atributos.putAll(novos);
  }

  /**
   * A referência do recurso que contém este.
   *
   * Guardada como texto, e não como objeto, porque o pai pode ser criado
   * depois — e porque apagá-lo não deve deixar um ponteiro pendurado. Quem
   * resolve é o {@link Cenario}, que é quem tem o mapa.
   */
  public String getPaiRef() {
    return paiRef;
  }

  public void setPaiRef(String paiRef) {
    this.paiRef = paiRef;
  }

  /** Quem resolve o pai — o cenário é quem tem o mapa. */
  void ligarAo(Cenario cenario) {
    this.cenario = cenario;
  }

  /**
   * A corrente que o {@code ContextResolver} sobe para publicar as chaves de
   * cada nível ({@code bucket:dono} a partir de um objeto dentro do bucket).
   *
   * Resolvida na hora, e não guardada: o pai pode ser criado depois deste, e
   * apagá-lo não pode deixar um ponteiro pendurado.
   */
  @Override
  public Resource getPai() {
    if (paiRef == null || cenario == null)
      return null;
    var pai = cenario.recurso(paiRef);
    return pai == this ? null : pai; // um recurso pai de si mesmo travaria a subida
  }

  /** A política anexada a este recurso — a bucket policy da AWS. */
  public Policy getPolitica() {
    return politica;
  }

  public void setPolitica(Policy politica) {
    this.politica = politica;
  }

  @Override
  public String toString() {
    return getRef();
  }
}
