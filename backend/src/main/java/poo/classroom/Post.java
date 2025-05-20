package poo.classroom;

import poo.iam.User;
import poo.iam.resources.ResourceTypes;

public class Post extends Publicacao {
  public Post(String titulo, String corpo, User autor, Turma turma) {
    this.titulo = titulo.strip();
    this.corpo = corpo.strip();
    this.autor = autor;
    this.turma = turma;
  }

  @Override
  public ResourceTypes getType() {
    return ResourceTypes.POST;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o)
      return true;
    if (!(o instanceof Post))
      return false;
    var other = (Post) o;
    return id.equals(other.getId());
  }

  @Override
  public int hashCode() {
    return id.hashCode();
  }
}
