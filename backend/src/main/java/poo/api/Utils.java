package poo.api;

import poo.api.exceptions.ForbiddenException;
import poo.api.exceptions.NotFoundException;
import poo.api.exceptions.UnauthorizedException;
import poo.iam.SecurityContext;
import poo.iam.SystemPermission;
import poo.iam.User;
import poo.iam.resources.Resource;
import io.javalin.http.Context;

public class Utils {

  public static boolean isAdmin(String uid) {
    var auth = SecurityContext.getInstance();
    var admin = auth.getAdmin();
    var user = uid.equals(admin.getId()) ? admin : UserController.getUser(uid);
    var isAdmin = user != null && auth.isAdmin(user);
    return isAdmin;
  }

  public static boolean isAdmin(User user) {
    return isAdmin(user.getId());
  }

  public static User findAuthUserOrThrow(Context ctx) {
    var uid = ctx.cookie("uid");
    if (uid == null)
      throw new UnauthorizedException();
    return findUserOrThrow(uid, "Usuário não encontrado");
  }

  public static User findUserOrThrow(String uid, String errorMessage) {
    var auth = SecurityContext.getInstance();
    User user = isAdmin(uid) ? auth.getAdmin() : UserController.getUser(uid);
    if (user == null)
      throw new NotFoundException(errorMessage);
    return user;
  }

  public static boolean hasPermissionOrThrow(Context ctx, SystemPermission permission, Resource resource,
      Object... args) {
    var user = findAuthUserOrThrow(ctx);
    if (!permission.isAllowed(user, resource, args))
      throw new ForbiddenException();
    return true;
  }

  public static boolean hasPermissionOrThrow(Context ctx, SystemPermission permission, Resource resource) {
    return hasPermissionOrThrow(ctx, permission, resource, new Object[0]);
  }

  public static boolean hasPermissionOrThrow(Context ctx, SystemPermission permission) {
    return hasPermissionOrThrow(ctx, permission, null, new Object[0]);
  }
}
