package poo.classroom;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import poo.iam.User;
import poo.iam.resources.Resource;

public abstract class Publicacao implements Resource {
  protected final String id = UUID.randomUUID().toString();
  protected String titulo;
  protected String corpo;
  protected User autor;
  protected Turma turma;
  protected List<Comentario> comentarios = new ArrayList<>();

  public String getId() {
    return id;
  }

  public String getTitulo() {
    return titulo;
  }

  public String getCorpo() {
    return corpo;
  }

  public void setCorpo(String corpo) {
    this.corpo = corpo;
  }

  public void setTitulo(String titulo) {
    this.titulo = titulo;
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
}
