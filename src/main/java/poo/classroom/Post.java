package poo.classroom;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import poo.iam.User;
import poo.iam.resources.Resource;

public class Post implements Resource {
  private final String id = UUID.randomUUID().toString();

  private String titulo;
  private String corpo;
  private User autor;
  private Turma turma;
  private List<Comentario> comentarios = new ArrayList<>();

  public Post(String corpo, User autor) {
    this.corpo = corpo.strip();
    this.autor = autor;
  }

  public String getId() {
    return id;
  }

  public String getTitulo() {
    return titulo;
  }

  public String getCorpo() {
    return corpo;
  }

  public User getAutor() {
    return autor;
  }

  public Turma getTurma() {
    return turma;
  }

  public List<Comentario> getComentarios() {
    return Collections.unmodifiableList(comentarios);
  }

  public void adicionarComentario(Comentario comentario) {
    comentarios.add(comentario);
  }

  @Override
  public String getType() {
    return "post";
  }
}
