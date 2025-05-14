package poo.iam.resources;

// public abstract class Resource {
//   protected String id;

//   public Resource() {
//     super();
//     id = UUID.randomUUID().toString();
//   }

//   public Resource(String resourceId) {
//     this();
//     if (!(resourceId == null))
//       id = resourceId;
//   }

//   public String getId() {
//     return id;
//   }

//   public abstract String getType();
// }

public interface Resource {
  String getType();
}