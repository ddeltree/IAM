package poo.classroom;

import poo.iam.User;
import poo.iam.resources.Resource;

public class Comentario implements Resource {
  private static long proximoId = 1; // contador global
  protected final String id = String.valueOf(proximoId++);
  private String conteudo;
  private User autor;
  private Post post;

  public Comentario(String conteudo, User autor, Post post) {
    this.conteudo = conteudo.strip();
    this.autor = autor;
    this.post = post;
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

  public Post getPost() {
    return post;
  }

  @Override
  public String getType() {
    return "comentario";
  }
}
