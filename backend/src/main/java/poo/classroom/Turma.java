package poo.classroom;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import poo.iam.User;
import poo.iam.resources.Resource;
import poo.iam.resources.ResourceTypes;

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

  public List<User> getParticipantes() {
    var res = new ArrayList<User>(alunos);
    res.add(professor);
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

  public void adicionarAluno(User aluno) {
    alunos.add(aluno);
  }

  public void removerAluno(User aluno) {
    alunos.remove(aluno);
  }

  public boolean temAluno(User aluno) {
    return alunos.contains(aluno);
  }

  public boolean temAluno(String uid) {
    return alunos.stream().anyMatch(aluno -> aluno.getId().equals(uid));
  }

  @Override
  public ResourceTypes getType() {
    return ResourceTypes.TURMA;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o)
      return true;
    if (!(o instanceof Turma))
      return false;
    Turma turma = (Turma) o;
    return id.equals(turma.getId());
  }

  @Override
  public int hashCode() {
    return id.hashCode();
  }
}
