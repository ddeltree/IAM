package poo.classroom;

import poo.iam.User;

public class Atividade extends Publicacao {
  public Atividade(String titulo, String corpo, User autor, Turma turma) {
    this.titulo = titulo.strip();
    this.corpo = corpo.strip();
    this.autor = autor;
    this.turma = turma;
  }

  @Override
  public String getType() {
    return "atividade";
  }
}
