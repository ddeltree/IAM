package poo.classroom;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

import poo.iam.User;
import poo.iam.resources.Resource;

public abstract class Publicacao implements Resource {
  private static long proximoId = 1;
  protected final String id = String.valueOf(proximoId++);
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

  /**
   * Ignorado na serialização: a turma lista as próprias publicações, e serializar
   * os dois lados faria Jackson entrar em recursão infinita. O JSON expõe
   * {@code turmaId} no lugar.
   */
  @JsonIgnore
  public Turma getTurma() {
    return turma;
  }

  @JsonProperty("turmaId")
  public String getTurmaId() {
    return turma == null ? null : turma.getId();
  }

  public List<Comentario> getComentarios() {
    return Collections.unmodifiableList(comentarios);
  }

  public void adicionarComentario(Comentario comentario) {
    comentarios.add(comentario);
  }

  public void removerComentario(Comentario comentario) {
    comentarios.remove(comentario);
  }

  /** Reinicia o contador de ids. Existe para isolar os cenários de teste. */
  public static void resetIdCounter() {
    proximoId = 1;
  }
}
