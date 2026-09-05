package poo.api;

import java.util.Set;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.ObjectMapper;

import poo.iam.Group;
import poo.iam.Permission;
import poo.iam.Resource;
import poo.iam.Statement;
import poo.iam.User;

/**
 * O que do núcleo não deve aparecer numa resposta HTTP.
 *
 * Isto morava como {@code @JsonIgnore} dentro do próprio núcleo, o que obrigava
 * quem o usasse a aceitar o Jackson junto. A anotação é um detalhe de quem
 * serializa, então desceu para cá — o núcleo voltou a não ter dependência
 * nenhuma, e outra aplicação pode esconder outros campos, ou nenhum.
 *
 * Duas coisas precisam sumir, e por motivos diferentes:
 *
 * <ul>
 *   <li><b>ciclos</b> — {@code User -> Group -> User} e {@code Post -> Turma ->
 *       Post} pela corrente de {@link Resource#getPai()} derrubam a
 *       serialização em recursão infinita;</li>
 *   <li><b>vazamento</b> — a política de um usuário é detalhe interno da
 *       autorização e apareceria em toda resposta que embute um usuário, como o
 *       autor de um post.</li>
 * </ul>
 */
public final class IamMixins {

  private IamMixins() {
  }

  /** A corrente de pais existe para as condições, não para o JSON. */
  public abstract static class RecursoMixin {
    @JsonIgnore
    public abstract Resource getPai();
  }

  public abstract static class UsuarioMixin {
    @JsonIgnore
    public abstract Set<Group> getGroups();

    @JsonIgnore
    public abstract Set<Statement> getStatements();

    @JsonIgnore
    public abstract Set<Permission> getInlinePermissions();

    @JsonIgnore
    public abstract Set<Permission> getDeniedPermissions();
  }

  public abstract static class GrupoMixin {
    @JsonIgnore
    public abstract Set<User> getUsers();
  }

  public static ObjectMapper aplicar(ObjectMapper mapper) {
    // no Resource, e não em cada implementação: assim turma, post, atividade e
    // comentário herdam a regra sem precisar ser listados um a um
    mapper.addMixIn(Resource.class, RecursoMixin.class);
    mapper.addMixIn(User.class, UsuarioMixin.class);
    mapper.addMixIn(Group.class, GrupoMixin.class);
    return mapper;
  }
}
