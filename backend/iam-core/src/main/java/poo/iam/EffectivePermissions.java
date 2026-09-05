package poo.iam;

import java.util.LinkedHashMap;
import java.util.Map;

import poo.iam.spi.ActionCatalog;

/**
 * Responde "o que este principal pode fazer aqui?" percorrendo o catálogo e
 * perguntando ao motor uma por uma — o mesmo que o
 * {@code SimulatePrincipalPolicy} da AWS faz, sem executar nada.
 *
 * É o que permite uma interface esconder um botão sem reescrever as regras do
 * lado dela. Enquanto essa resposta não existia, o frontend deste sistema
 * mantinha dezenove regras copiadas do backend em TypeScript — duas fontes de
 * verdade que podiam divergir em silêncio, e uma já estava posicionada para
 * divergir.
 *
 * As chaves do mapa são o nome da ação, e não o par ação-tipo: é o que a
 * interface pergunta ({@code pode('EDITAR_POST')}), e o filtro por tipo em
 * {@link #sobre} já garante que duas permissões de mesma ação e tipos
 * diferentes não apareçam juntas.
 */
public final class EffectivePermissions {

  private final AuthorizationEngine motor;
  private final ActionCatalog catalogo;

  public EffectivePermissions(AuthorizationEngine motor, ActionCatalog catalogo) {
    this.motor = motor;
    this.catalogo = catalogo;
  }

  /** As ações sem alvo. */
  public Map<String, Boolean> globais(Principal principal) {
    var res = new LinkedHashMap<String, Boolean>();
    for (Permission permissao : catalogo.semAlvo())
      res.put(chave(permissao), motor.isAllowed(principal, permissao, null));
    return res;
  }

  /**
   * As ações que se aplicam a este recurso. Só entram as do tipo dele:
   * perguntar por EDITAR_POST sobre uma turma não é uma pergunta.
   */
  public Map<String, Boolean> sobre(Principal principal, Resource recurso) {
    var res = new LinkedHashMap<String, Boolean>();
    for (Permission permissao : aplicaveis(recurso))
      res.put(chave(permissao), motor.isAllowed(principal, permissao, recurso));
    return res;
  }

  /** Como {@link #sobre}, mas dizendo qual cláusula decidiu cada uma. */
  public Map<String, Decisao> explicadas(Principal principal, Resource recurso) {
    var res = new LinkedHashMap<String, Decisao>();
    for (Permission permissao : aplicaveis(recurso))
      res.put(chave(permissao), motor.avaliar(principal, permissao, recurso));
    return res;
  }

  public Map<String, Decisao> globaisExplicadas(Principal principal) {
    var res = new LinkedHashMap<String, Decisao>();
    for (Permission permissao : catalogo.semAlvo())
      res.put(chave(permissao), motor.avaliar(principal, permissao, null));
    return res;
  }

  private java.util.List<Permission> aplicaveis(Resource recurso) {
    var semAlvo = catalogo.semAlvo();
    return catalogo.todas().stream()
        .filter(p -> !semAlvo.contains(p))
        .filter(p -> p.getResourceType().name().equals(recurso.getType().name()))
        .toList();
  }

  private static String chave(Permission permissao) {
    return permissao.getAction().name();
  }
}
