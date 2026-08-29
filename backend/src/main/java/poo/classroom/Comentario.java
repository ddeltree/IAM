package poo.classroom;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

import poo.iam.User;
import poo.iam.Resource;
import poo.iam.ResourceType;
import poo.classroom.iam.ClassroomResource;

public class Comentario implements Resource {
  private static long proximoId = 1; // contador global
  protected final String id = String.valueOf(proximoId++);
  private String conteudo;
  private User autor;
  private Publicacao publicacao;

  public Comentario(String conteudo, User autor, Publicacao post) {
    this.conteudo = conteudo.strip();
    this.autor = autor;
    this.publicacao = post;
  }

  public String getId() {
    return id;
  }

  public void editar(String conteudo) {
    this.conteudo = conteudo.strip();
  }

  public String getConteudo() {
    return conteudo;
  }

  public void setConteudo(String conteudo) {
    this.conteudo = conteudo.strip();
  }

  public User getAutor() {
    return autor;
  }

  /** Ignorado na serialização pelo mesmo motivo de {@link Publicacao#getTurma()}. */
  @JsonIgnore
  public Publicacao getPublicacao() {
    return publicacao;
  }

  @JsonProperty("publicacaoId")
  public String getPublicacaoId() {
    return publicacao == null ? null : publicacao.getId();
  }

  /** Reinicia o contador de ids. Existe para isolar os cenários de teste. */
  public static void resetIdCounter() {
    proximoId = 1;
  }

  @Override
  public ResourceType getType() {
    return ClassroomResource.COMENTARIO;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o)
      return true;
    if (!(o instanceof Comentario))
      return false;
    var other = (Comentario) o;
    return id.equals(other.getId());
  }

  @Override
  public int hashCode() {
    return id.hashCode();
  }
}
