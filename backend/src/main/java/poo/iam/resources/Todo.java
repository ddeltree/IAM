package poo.iam.resources;

public class Todo implements Resource {
  public Todo() {
    super();
  }

  @Override
  public String getType() {
    return ResourceTypes.TODO.name();
  }
}
