package poo.api;

import poo.api.exceptions.ForbiddenException;
import poo.api.exceptions.NotFoundException;
import poo.iam.PermissionService;
import poo.iam.SecurityContext;
import poo.iam.SystemPermission;
import poo.iam.User;

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

  public static User findAuthUserOr404(Context ctx) {
    var uid = ctx.cookie("uid");
    var auth = SecurityContext.getInstance();
    User user = isAdmin(uid) ? auth.getAdmin() : UserController.getUser(uid);
    if (user == null)
      throw new NotFoundException("Usuário não encontrado");
    return user;
  }

  public static boolean hasPermissionOrThrow(SystemPermission permission, Context ctx) {
    var user = findAuthUserOr404(ctx);
    if (!PermissionService.hasPermission(user, permission.get()))
      throw new ForbiddenException();
    return true;
  }
}
