package poo.classroom;

import poo.iam.User;

public class Atividade extends Publicacao {
  private String dataEntrega;

  public Atividade(String titulo, String corpo, Turma turma) {
    this.titulo = titulo.strip();
    this.corpo = corpo.strip();
    this.autor = turma.getProfessorResponsavel();
    this.turma = turma;
  }

  public String getDataEntrega() {
    return dataEntrega;
  }

  public void setDataEntrega(String dataEntrega) {
    this.dataEntrega = dataEntrega;
  }

  @Override
  public String getType() {
    return "atividade";
  }
}
