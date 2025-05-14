package poo.iam.resources;

public class ResourceFactory {
  public static Resource createResource(ResourceTypes type) {
    switch (type) {
      case DOCUMENT:
        return new Document();
      case TODO:
        return new Todo();
      default:
        throw new IllegalArgumentException("Unknown resource type");
    }
  }
}
