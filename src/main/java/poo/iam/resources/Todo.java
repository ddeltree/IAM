package poo.iam.resources;

public class Todo extends Resource {
  public Todo() {
    super();
  }

  public Todo(String resourceId) {
    super(resourceId);
  }

  @Override
  public String getType() {
    return ResourceTypes.TODO.name();
  }
}
