package poo.console.api;

import java.util.Set;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.ObjectMapper;

import poo.iam.Group;
import poo.iam.Permission;
import poo.iam.Resource;
import poo.iam.Statement;
import poo.iam.User;

/**
 * O que do núcleo não deve sair numa resposta HTTP.
 *
 * Cópia deliberada do mixin do classroom, e não uma classe compartilhada: os
 * dois módulos são aplicações independentes do mesmo componente, e o que cada
 * uma esconde é decisão dela. Compartilhar isto criaria uma dependência entre
 * duas aplicações que não se conhecem — e desfaria metade do que o segundo
 * módulo prova.
 */
public final class IamMixins {

  private IamMixins() {
  }

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
    mapper.addMixIn(Resource.class, RecursoMixin.class);
    mapper.addMixIn(User.class, UsuarioMixin.class);
    mapper.addMixIn(Group.class, GrupoMixin.class);
    return mapper;
  }
}
