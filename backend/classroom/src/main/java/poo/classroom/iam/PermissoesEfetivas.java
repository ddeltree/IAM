package poo.classroom.iam;

import java.util.LinkedHashMap;
import java.util.Map;

import poo.classroom.iam.ClassroomPermission.Escopo;
import poo.iam.Decisao;
import poo.iam.Resource;
import poo.iam.User;

/**
 * Responde "o que este usuário pode fazer aqui?" percorrendo o catálogo e
 * avaliando cada permissão — o mesmo que o {@code SimulatePrincipalPolicy} da
 * AWS faz, sem executar nada.
 *
 * É o que permite a interface esconder um botão sem reescrever as regras do
 * lado dela.
 */
public final class PermissoesEfetivas {

  private PermissoesEfetivas() {
  }

  /** As ações sem alvo: criar turma, criar usuário, listar usuários. */
  public static Map<String, Boolean> globais(User user) {
    var res = new LinkedHashMap<String, Boolean>();
    for (var permissao : ClassroomPermission.values()) {
      if (permissao.getEscopo() != Escopo.GLOBAL)
        continue;
      res.put(permissao.name(), permissao.isAllowed(user));
    }
    return res;
  }

  /**
   * As ações que se aplicam a este recurso. Só entram as permissões do tipo
   * dele: perguntar por EDITAR_POST sobre uma turma não é uma pergunta.
   */
  public static Map<String, Boolean> sobre(User user, Resource recurso) {
    var res = new LinkedHashMap<String, Boolean>();
    for (var permissao : ClassroomPermission.values()) {
      if (!aplicavel(permissao, recurso))
        continue;
      res.put(permissao.name(), permissao.isAllowed(user, recurso));
    }
    return res;
  }

  /** Como {@link #sobre}, mas dizendo qual cláusula decidiu cada uma. */
  public static Map<String, Decisao> explicadas(User user, Resource recurso) {
    var res = new LinkedHashMap<String, Decisao>();
    for (var permissao : ClassroomPermission.values()) {
      if (!aplicavel(permissao, recurso))
        continue;
      res.put(permissao.name(), permissao.avaliar(user, recurso));
    }
    return res;
  }

  public static Map<String, Decisao> globaisExplicadas(User user) {
    var res = new LinkedHashMap<String, Decisao>();
    for (var permissao : ClassroomPermission.values()) {
      if (permissao.getEscopo() != Escopo.GLOBAL)
        continue;
      res.put(permissao.name(), permissao.avaliar(user, null));
    }
    return res;
  }

  private static boolean aplicavel(ClassroomPermission permissao, Resource recurso) {
    return permissao.getEscopo() == Escopo.RECURSO
        && permissao.getResourceType().equals(recurso.getType());
  }
}
