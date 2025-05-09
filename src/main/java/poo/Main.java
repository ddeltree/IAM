package poo;

import poo.src.*;
import poo.src.resources.*;

public class Main {
    public static void main(String[] args) {
        // Criando e atribuindo permissões
        var viewDocPermission = new Permission(Action.VIEW, ResourceTypes.DOCUMENT);
        var deleteDocPermission = new Permission(Action.DELETE, ResourceTypes.DOCUMENT);
        Resource doc = ResourceFactory.createResource(ResourceTypes.DOCUMENT, "doc1");
        Resource doc2 = ResourceFactory.createResource(ResourceTypes.DOCUMENT, "doc2");
        Resource todo = ResourceFactory.createResource(ResourceTypes.TODO, "todo");

        User commonUser = new User("user1");
        User adminUser = new User("admin1");

        System.out.println("Admin user can delete document: " + adminUser.hasInlinePermission(deleteDocPermission));
        adminUser.grantPermission(deleteDocPermission);
        System.out.println("Admin user can delete document: " + adminUser.hasInlinePermission(deleteDocPermission));

        boolean canView = PermissionService.hasPermission(commonUser, doc, Action.VIEW);
        System.out.println("Common user can view document: " + canView);

        Group adminGroup = new Group("Admin");
        adminGroup.grantPermission(viewDocPermission);
        System.out.println(
                "Admin user can view document: " + PermissionService.hasPermission(adminUser, doc, Action.VIEW)); // Output:
        MembershipManager.link(adminUser, adminGroup);
        System.out.println("Admin user can view document: " + PermissionService.hasPermission(adminUser, doc,
                Action.VIEW)); // Output:
        System.out.println("Common user can delete document: " + PermissionService.hasPermission(commonUser, doc2,
                Action.VIEW)); // Output:
    }
}
