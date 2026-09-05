package poo.api;

import poo.api.exceptions.ForbiddenException;
import poo.api.exceptions.NotFoundException;
import poo.api.exceptions.UnauthorizedException;
import poo.classroom.iam.SecurityContext;
import poo.classroom.iam.ClassroomPermission;
import poo.iam.User;
import poo.iam.Resource;
import java.util.List;
import java.util.Map;

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

  public static boolean hasPermissionOrThrow(Context ctx, ClassroomPermission permission,
      Resource resource) {
    var user = findAuthUserOrThrow(ctx);
    if (!permission.isAllowed(user, resource, chavesDaRequisicao(ctx)))
      throw new ForbiddenException();
    return true;
  }

  public static boolean hasPermissionOrThrow(Context ctx, ClassroomPermission permission) {
    return hasPermissionOrThrow(ctx, permission, null);
  }

  /**
   * O que só esta camada sabe sobre o pedido.
   *
   * O núcleo já publica principal, recurso e horário; a origem e o método são
   * do HTTP, e ele não tem como adivinhá-los. Chegam às condições como
   * {@code requisicao:ip} e {@code requisicao:metodo} — o equivalente às chaves
   * {@code aws:SourceIp} e {@code aws:*} que o serviço acrescenta ao pedido.
   *
   * Nenhuma política daqui as usa ainda. Estão publicadas porque o custo é uma
   * linha e a alternativa é descobrir, quando precisar de "só da rede da
   * escola", que o dado nunca chegou até onde a decisão acontece.
   */
  private static Map<String, List<String>> chavesDaRequisicao(Context ctx) {
    return Map.of(
        "ip", List.of(String.valueOf(ctx.ip())),
        "metodo", List.of(ctx.method().name()),
        "caminho", List.of(ctx.path()));
  }
}
