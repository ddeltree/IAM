package poo.iam;

public class MembershipManager {
  public static void link(User user, Group group) {
    if (user.getGroups().contains(group))
      return;
    user.joinGroup(group);
    group.addUser(user);
  }

  public static void unlink(User user, Group group) {
    if (!user.getGroups().contains(group))
      return;
    user.quitGroup(group);
    group.removeUser(user);
  }
}
