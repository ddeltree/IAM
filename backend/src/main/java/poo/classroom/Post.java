package poo.classroom;

import poo.iam.User;

public class Post extends Publicacao {
  public Post(String titulo, String corpo, User autor, Turma turma) {
    this.titulo = titulo.strip();
    this.corpo = corpo.strip();
    this.autor = autor;
    this.turma = turma;
  }

  @Override
  public String getType() {
    return "post";
  }
}
