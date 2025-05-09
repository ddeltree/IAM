package poo.src.resources;

public class Document extends Resource {
  public Document(String resourceId) {
    this.resourceId = resourceId;
  }

  @Override
  public ResourceTypes getType() {
    return ResourceTypes.DOCUMENT;
  }
}
