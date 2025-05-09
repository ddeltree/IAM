package poo.src.resources;

public class Todo extends Resource {
  public Todo(String resourceId) {
    this.resourceId = resourceId;
  }

  @Override
  public ResourceTypes getType() {
    return ResourceTypes.TODO;
  }
}
