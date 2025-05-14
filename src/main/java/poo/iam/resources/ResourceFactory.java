package poo.iam.resources;

public class ResourceFactory {
  public static Resource createResource(ResourceTypes type, String resourceId) {
    switch (type) {
      case DOCUMENT:
        return new Document(resourceId);
      case TODO:
        return new Todo(resourceId);
      default:
        throw new IllegalArgumentException("Unknown resource type");
    }
  }

  public static Resource createResource(ResourceTypes type) {
    return createResource(type, null);
  }
}
