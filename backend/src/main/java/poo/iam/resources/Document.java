package poo.iam.resources;

public class Document implements Resource {
  public Document() {
    super();
  }

  @Override
  public String getType() {
    return ResourceTypes.DOCUMENT.name();
  }

}
