package poo.classroom;

import poo.iam.User;

public class Post extends Publicacao {
  public Post(String corpo, User autor) {
    this.corpo = corpo.strip();
    this.autor = autor;
  }

  @Override
  public String getType() {
    return "post";
  }
}
