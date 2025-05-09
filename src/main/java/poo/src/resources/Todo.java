package poo.src.resources;

public class Todo extends Resource {
  public Todo() {
    super();
  }

  public Todo(String resourceId) {
    super(resourceId);
  }

  @Override
  public ResourceTypes getType() {
    return ResourceTypes.TODO;
  }
}
