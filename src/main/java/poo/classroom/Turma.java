package poo.classroom;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import poo.iam.User;
import poo.iam.resources.Resource;

public class Turma implements Resource {
  private static long proximoId = 1; // contador global
  private final String id;
  private String nome;
  private User professor;
  private List<User> alunos = new ArrayList<>();
  private List<Post> posts = new ArrayList<>();

  public Turma(String nome, User professor) {
    this.id = String.valueOf(proximoId++);
    this.nome = nome;
    this.professor = professor;
  }

  public String getId() {
    return id;
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

  public List<Post> getPosts() {
    return Collections.unmodifiableList(posts);
  }

  public void adicionarPost(Post post) {
    posts.add(post);
  }

  @Override
  public String getType() {
    return "turma";
  }
}
