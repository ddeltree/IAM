package poo.src.resources;

public abstract class Resource {
  protected String resourceId;

  public String getResourceId() {
    return resourceId;
  }

  public abstract ResourceTypes getType();
}