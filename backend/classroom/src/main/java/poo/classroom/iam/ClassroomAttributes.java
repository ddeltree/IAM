package poo.classroom.iam;

import java.util.List;
import java.util.Map;

import poo.classroom.Comentario;
import poo.classroom.Publicacao;
import poo.classroom.Turma;
import poo.iam.spi.AttributeProvider;
import poo.iam.ContextResolver;
import poo.iam.PrincipalResource;
import poo.iam.Resource;
import poo.iam.ResourceType;
import poo.iam.User;

/**
 * Traduz os objetos desta aplicação nas chaves de condição que as políticas
 * leem.
 *
 * É o que permite o núcleo avaliar "o autor é quem está pedindo?" sem nunca ter
 * ouvido falar em post ou em turma. As chaves saem sem prefixo; quem qualifica
 * com o tipo é o {@link ContextResolver}.
 */
public final class ClassroomAttributes {

  private ClassroomAttributes() {
  }

  /** Registra todos os provedores. Chamado na criação e no reset do contexto. */
  public static void registrarTodos(ContextResolver resolver) {
    resolver.registrar(provedor(ClassroomResource.TURMA, ClassroomAttributes::daTurma));
    resolver.registrar(provedor(ClassroomResource.POST, ClassroomAttributes::daPublicacao));
    resolver.registrar(provedor(ClassroomResource.ATIVIDADE, ClassroomAttributes::daPublicacao));
    resolver.registrar(provedor(ClassroomResource.COMENTARIO, ClassroomAttributes::doComentario));
    resolver.registrar(provedor(PrincipalResource.USUARIO, ClassroomAttributes::doUsuario));
  }

  private static Map<String, List<String>> daTurma(Resource recurso) {
    var turma = (Turma) recurso;
    return Map.of(
        "id", List.of(turma.getId()),
        "professorId", List.of(turma.getProfessorResponsavel().getId()),
        "alunoIds", turma.getAlunos().stream().map(User::getId).toList());
  }

  private static Map<String, List<String>> daPublicacao(Resource recurso) {
    var publicacao = (Publicacao) recurso;
    return Map.of(
        "id", List.of(publicacao.getId()),
        "autorId", List.of(publicacao.getAutor().getId()));
  }

  private static Map<String, List<String>> doComentario(Resource recurso) {
    var comentario = (Comentario) recurso;
    return Map.of(
        "id", List.of(comentario.getId()),
        "autorId", List.of(comentario.getAutor().getId()));
  }

  private static Map<String, List<String>> doUsuario(Resource recurso) {
    var user = (User) recurso;
    return Map.of("id", List.of(user.getId()));
  }

  private static AttributeProvider provedor(ResourceType tipo, Extrator extrator) {
    return new AttributeProvider() {
      @Override
      public ResourceType tipo() {
        return tipo;
      }

      @Override
      public Map<String, List<String>> atributosDe(Resource recurso) {
        return extrator.extrair(recurso);
      }
    };
  }

  @FunctionalInterface
  private interface Extrator {
    Map<String, List<String>> extrair(Resource recurso);
  }
}
