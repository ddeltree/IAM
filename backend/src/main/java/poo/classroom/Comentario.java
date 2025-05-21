package poo.classroom;

import poo.iam.User;
import poo.iam.resources.Resource;
import poo.iam.resources.ResourceTypes;

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

  public Publicacao getPublicacao() {
    return publicacao;
  }

  @Override
  public ResourceTypes getType() {
    return ResourceTypes.COMENTARIO;
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
