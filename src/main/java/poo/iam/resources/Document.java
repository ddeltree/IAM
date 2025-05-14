package poo.iam.resources;

public class Document extends Resource {
  public Document() {
    super();
  }

  public Document(String resourceId) {
    super(resourceId);
  }

  @Override
  public String getType() {
    return ResourceTypes.DOCUMENT.name();
  }
}
