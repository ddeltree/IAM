package poo.src.resources;

public class Document extends Resource {
  public Document() {
    super();
  }

  public Document(String resourceId) {
    super(resourceId);
  }

  @Override
  public ResourceTypes getType() {
    return ResourceTypes.DOCUMENT;
  }
}
