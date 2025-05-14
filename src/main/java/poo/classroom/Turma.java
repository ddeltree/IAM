package poo.classroom;

import java.util.ArrayList;
import java.util.List;

import poo.iam.User;
import poo.iam.resources.Resource;

public class Turma implements Resource {
  private String nome;
  private User professor;
  private List<User> alunos = new ArrayList<>();

  public Turma(String nome, User professor) {
    this.nome = nome;
    this.professor = professor;
  }

  public User getProfessorResponsavel() {
    return professor;
  }

  public List<User> getAlunos() {
    return alunos;
  }

  public String getNome() {
    return nome;
  }

  public Turma setNome(String nome) {
    this.nome = nome;
    return this;
  }

  public Turma setProfessor(User professor) {
    this.professor = professor;
    return this;
  }

  @Override
  public String getType() {
    return "turma";
  }
}
