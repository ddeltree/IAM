package poo.console;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import poo.iam.Group;
import poo.iam.Iam;
import poo.iam.IamFactory;
import poo.iam.Permission;
import poo.iam.Policy;
import poo.iam.Principal;
import poo.iam.Resource;
import poo.iam.ResourceType;
import poo.iam.Role;
import poo.iam.Session;
import poo.iam.User;
import poo.iam.spi.ActionCatalog;
import poo.iam.spi.AttributeProvider;
import poo.iam.spi.PrincipalDirectory;
import poo.iam.spi.ResourcePolicyProvider;

/**
 * Tudo o que existe neste console: o vocabulário, os principais, as políticas e
 * os recursos.
 *
 * A diferença para o {@code SecurityContext} do classroom é que lá nada disso
 * muda em tempo de execução — os papéis são três, as permissões são vinte e
 * seis, e a política vem de um documento na inicialização. Aqui tudo é criado
 * pela tela, e é isso que faz este módulo exercitar o núcleo inteiro.
 *
 * <h2>O componente é montado uma vez</h2>
 *
 * O {@link Iam} lê destes mapas <em>por referência</em>: o catálogo, o
 * diretório de principais e o provedor de políticas de recurso são lambdas
 * sobre eles. Criar um usuário ou uma ação não exige remontar nada — o que
 * seria inviável numa tela onde cada clique muda a configuração.
 */
public final class Cenario {

  private static Cenario atual = SementeDoCenario.montar();

  /** O cenário em uso. Estático como o do classroom, e pelo mesmo motivo. */
  public static Cenario atual() {
    return atual;
  }

  /** Substitui o cenário — usado por importar e reiniciar. */
  public static void substituir(Cenario novo) {
    atual = novo;
  }

  public static void reiniciar() {
    atual = SementeDoCenario.montar();
  }

  // ---------- o que existe ----------

  private final Map<String, User> usuarios = new LinkedHashMap<>();
  private final Map<String, Group> grupos = new LinkedHashMap<>();
  private final Map<String, Role> papeis = new LinkedHashMap<>();
  private final Map<String, Policy> politicas = new LinkedHashMap<>();
  private final Map<String, RecursoLivre> recursos = new LinkedHashMap<>();
  private final Map<String, Session> sessoes = new LinkedHashMap<>();

  private final Set<String> acoes = new LinkedHashSet<>();
  private final Set<String> tipos = new LinkedHashSet<>();
  private final Set<Permission> permissoes = new LinkedHashSet<>();
  private final Set<Permission> semAlvo = new LinkedHashSet<>();

  private final Iam iam;

  public Cenario() {
    this.iam = IamFactory.novo()
        // um provedor só, para todos os tipos: cada recurso já sabe os seus.
        // Com tipos nascendo na tela, registrar um por tipo criado seria
        // manutenção de mapa a cada clique
        .atributos()
        .catalogo(new ActionCatalog() {
          public Collection<Permission> todas() {
            return permissoes;
          }

          @Override
          public Collection<Permission> semAlvo() {
            return semAlvo;
          }
        })
        .principais(new PrincipalDirectory() {
          public Collection<User> usuarios() {
            return usuarios.values();
          }

          public Collection<Group> grupos() {
            return grupos.values();
          }

          @Override
          public Collection<Role> papeis() {
            return papeis.values();
          }
        })
        .politicasDeRecurso(new ResourcePolicyProvider() {
          public Policy politicaDe(Resource recurso) {
            var meu = recursos.get(referencia(recurso));
            return meu == null ? null : meu.getPolitica();
          }

          @Override
          public Collection<Resource> comPoliticaPropria(ResourceType tipo) {
            return recursos.values().stream()
                .filter(r -> r.getPolitica() != null)
                .filter(r -> r.getTipo().equals(tipo.name()))
                .map(r -> (Resource) r)
                .toList();
          }
        })
        .construir();

    iam.contexto().registrarPadrao(new AttributeProvider() {
      public ResourceType tipo() {
        return new TipoLivre("*"); // ignorado: como padrão, ele atende qualquer tipo
      }

      public Map<String, List<String>> atributosDe(Resource recurso) {
        return recurso instanceof RecursoLivre livre ? livre.getAtributos() : Map.of();
      }
    });
  }

  public Iam iam() {
    return iam;
  }

  private static String referencia(Resource recurso) {
    return recurso.getType().name() + "/" + recurso.getId();
  }

  // ---------- principais ----------

  public User criarUsuario(String id, String nome) {
    exigirLivre(id, "usuário");
    var user = new User(id, nome);
    usuarios.put(id, user);
    return user;
  }

  public Group criarGrupo(String id, String nome) {
    exigirLivre(id, "grupo");
    var grupo = new Group(id, nome);
    grupos.put(id, grupo);
    return grupo;
  }

  public Role criarPapel(String id, String nome) {
    exigirLivre(id, "papel");
    var papel = new Role(id, nome);
    papeis.put(id, papel);
    return papel;
  }

  /**
   * Um id vale para usuários, grupos e papéis ao mesmo tempo.
   *
   * O núcleo não exigiria isso — ele distingue principais por identidade. Mas
   * a tela os endereça por id numa URL, e dois principais com o mesmo id
   * fariam o console responder pelo errado.
   */
  private void exigirLivre(String id, String oQue) {
    if (id == null || id.isBlank())
      throw new IllegalArgumentException("Um " + oQue + " precisa de identificador");
    if (usuarios.containsKey(id) || grupos.containsKey(id) || papeis.containsKey(id))
      throw new IllegalArgumentException("Já existe um principal com o id " + id);
  }

  public Principal principal(String id) {
    if (usuarios.containsKey(id))
      return usuarios.get(id);
    if (grupos.containsKey(id))
      return grupos.get(id);
    if (papeis.containsKey(id))
      return papeis.get(id);
    return sessoes.get(id);
  }

  public User usuario(String id) {
    return usuarios.get(id);
  }

  public Group grupo(String id) {
    return grupos.get(id);
  }

  public Role papel(String id) {
    return papeis.get(id);
  }

  public Session sessao(String id) {
    return sessoes.get(id);
  }

  public Collection<User> usuarios() {
    return usuarios.values();
  }

  public Collection<Group> grupos() {
    return grupos.values();
  }

  public Collection<Role> papeis() {
    return papeis.values();
  }

  public Collection<Session> sessoes() {
    return sessoes.values();
  }

  public void guardarSessao(Session sessao) {
    sessoes.put(sessao.getId(), sessao);
  }

  public boolean largarSessao(String id) {
    return sessoes.remove(id) != null;
  }

  public boolean apagarPrincipal(String id) {
    var user = usuarios.remove(id);
    if (user != null) {
      // sair dos grupos junto: um grupo guardando um usuário apagado o traria
      // de volta nas consultas reversas
      for (Group grupo : grupos.values())
        poo.iam.MembershipManager.unlink(user, grupo);
      sessoes.values().removeIf(s -> s.getOrigem() == user);
      return true;
    }
    if (grupos.remove(id) != null)
      return true;
    if (papeis.remove(id) != null) {
      sessoes.values().removeIf(s -> s.getPapel().getId().equals(id));
      return true;
    }
    return false;
  }

  // ---------- políticas ----------

  public Collection<Policy> politicas() {
    return politicas.values();
  }

  public Policy politica(String nome) {
    return politicas.get(nome);
  }

  public void salvarPolitica(Policy policy) {
    politicas.put(policy.getNome(), policy);
  }

  /**
   * Apaga uma política, desanexando-a de quem a tinha.
   *
   * Sem desanexar, ela continuaria valendo para todos — invisível na tela e
   * ativa no motor, que é a pior combinação possível.
   */
  public boolean apagarPolitica(String nome) {
    var policy = politicas.remove(nome);
    if (policy == null)
      return false;
    usuarios.values().forEach(u -> u.desanexar(policy));
    grupos.values().forEach(g -> g.desanexar(policy));
    return true;
  }

  // ---------- recursos ----------

  public Collection<RecursoLivre> recursos() {
    return recursos.values();
  }

  public RecursoLivre recurso(String ref) {
    return recursos.get(ref);
  }

  public RecursoLivre salvarRecurso(RecursoLivre recurso) {
    recurso.ligarAo(this);
    recursos.put(recurso.getRef(), recurso);
    tipos.add(recurso.getTipo());
    return recurso;
  }

  public boolean apagarRecurso(String ref) {
    return recursos.remove(ref) != null;
  }

  // ---------- vocabulário ----------

  public Set<String> acoes() {
    return acoes;
  }

  public Set<String> tipos() {
    return tipos;
  }

  public Collection<Permission> permissoes() {
    return permissoes;
  }

  public Collection<Permission> semAlvo() {
    return semAlvo;
  }

  public void declararAcao(String acao) {
    acoes.add(acao);
  }

  public void declararTipo(String tipo) {
    tipos.add(tipo);
  }

  /**
   * Declara que esta ação faz sentido sobre este tipo.
   *
   * Não é o produto cartesiano de propósito: cruzar tudo encheria o simulador e
   * as permissões efetivas de perguntas sem sentido ({@code LER} sobre
   * {@code USUARIO}), e o catálogo deixaria de ser uma lista do que existe para
   * virar uma lista do que é combinável.
   */
  public Permission declararPermissao(String acao, String tipo, boolean semAlvoTambem) {
    acoes.add(acao);
    tipos.add(tipo);
    var permissao = new Permission(new AcaoLivre(acao), new TipoLivre(tipo));
    permissoes.add(permissao);
    if (semAlvoTambem)
      semAlvo.add(permissao);
    return permissao;
  }

  public boolean removerPermissao(String acao, String tipo) {
    var permissao = new Permission(new AcaoLivre(acao), new TipoLivre(tipo));
    semAlvo.remove(permissao);
    return permissoes.remove(permissao);
  }

  public Permission permissao(String acao, String tipo) {
    var procurada = new Permission(new AcaoLivre(acao), new TipoLivre(tipo));
    return permissoes.contains(procurada) ? procurada : null;
  }

  /**
   * As chaves de contexto que uma condição pode ler neste cenário.
   *
   * É o que torna o editor de condição usável: sem esta lista, escrever uma
   * condição seria adivinhar o nome da chave, e uma chave errada avalia falso
   * silenciosamente — parecendo bug em vez de erro de digitação.
   */
  public List<String> chavesDisponiveis() {
    var chaves = new LinkedHashSet<String>(List.of(
        "principal:id", "principal:name", "principal:groups",
        "recurso:id", "recurso:tipo",
        "contexto:instante", "contexto:data", "contexto:hora",
        "sessao:papel", "sessao:origem"));

    for (RecursoLivre recurso : recursos.values()) {
      for (String atributo : recurso.getAtributos().keySet()) {
        chaves.add("recurso:" + atributo);
        chaves.add(recurso.getTipo().toLowerCase() + ":" + atributo);
      }
    }
    return new ArrayList<>(chaves);
  }
}
