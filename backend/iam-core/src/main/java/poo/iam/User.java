package poo.iam;

import java.util.*;

import poo.iam.condition.Condition;
import poo.iam.spi.PolicyListener;
import poo.iam.spi.PolicyListener.Mudanca;

public class User implements Principal, Resource {
  private static long proximoId = 1; // contador global
  private final String id;
  private String name;
  private final Set<Group> groups = new HashSet<>();
  private final PermissionHolder policy = new PermissionHolder(); // composition
  private PolicyListener ouvinte = PolicyListener.SILENCIOSO;

  public User() {
    this(proximoId(), null);
  }

  public User(String name) {
    this(proximoId(), name);
  }

  /**
   * Com o id vindo de fora.
   *
   * O contador desta classe é uma conveniência para quem não tem um id
   * próprio, não uma imposição: uma aplicação com chave de banco, UUID ou o
   * {@code sub} de um token constrói o usuário com o identificador que já usa,
   * e o núcleo não precisa saber de onde ele veio.
   */
  public User(String id, String name) {
    this.id = id;
    this.name = name;
  }

  private static String proximoId() {
    return String.valueOf(proximoId++);
  }

  /**
   * Quem é avisado quando esta política muda. O padrão é ninguém: uma
   * biblioteca não escreve na saída de quem a usa.
   */
  public User comOuvinte(PolicyListener ouvinte) {
    this.ouvinte = ouvinte == null ? PolicyListener.SILENCIOSO : ouvinte;
    return this;
  }

  // PERMISSIONS

  /** Só as cláusulas próprias, sem as das políticas anexadas. */
  public Set<Statement> getStatementsInline() {
    return policy.getStatementsInline();
  }

  /**
   * Anexa uma política nomeada — a <em>managed policy</em> da AWS.
   *
   * As cláusulas dela passam a valer sem serem copiadas: mudar a política muda
   * o acesso de todos que a têm anexada, de uma vez.
   */
  public boolean anexar(Policy policy) {
    return policy != null && this.policy.anexar(policy);
  }

  public boolean desanexar(Policy policy) {
    return this.policy.desanexar(policy);
  }

  public java.util.List<Policy> getPoliticasAnexadas() {
    return this.policy.getPoliticasAnexadas();
  }

  /**
   * Acrescenta uma cláusula pronta.
   *
   * É por aqui que entram as que não cabem em {@code grantPermission}: as com
   * curinga, as que miram um recurso específico e as que vêm de um documento
   * carregado em vez do código.
   */
  public boolean add(Statement statement) {
    return registrar(policy.add(statement), Mudanca.CONCEDIDA, statement.getPermission());
  }

  /** Concessão irrestrita. */
  public boolean grantPermission(Permission permission) {
    return registrar(policy.grant(permission), Mudanca.CONCEDIDA, permission);
  }

  /** Concessão válida só quando a condição passar. */
  public boolean grantPermission(Permission permission, Condition condition) {
    return registrar(policy.grant(permission, condition), Mudanca.CONCEDIDA, permission);
  }

  public boolean revokePermission(Permission permission) {
    return registrar(policy.revoke(permission), Mudanca.REVOGADA, permission);
  }

  /**
   * Nega a permissão para este usuário, sobrepondo o que os grupos dele
   * concedem.
   */
  public boolean denyPermission(Permission permission) {
    return registrar(policy.deny(permission), Mudanca.NEGADA, permission);
  }

  public boolean denyPermission(Permission permission, Condition condition) {
    return registrar(policy.deny(permission, condition), Mudanca.NEGADA, permission);
  }

  /**
   * Remove a negação explícita. Não concede nada por si só — o usuário volta a
   * depender das permissões inline e das herdadas dos grupos.
   */
  public boolean allowPermission(Permission permission) {
    return registrar(policy.allow(permission), Mudanca.NEGACAO_REMOVIDA, permission);
  }

  /**
   * Avisa o ouvinte, se houver. A permissão vem {@code null} quando a cláusula
   * usa curinga — não há uma permissão para nomear, e mentir uma seria pior.
   */
  private boolean registrar(boolean mudou, Mudanca mudanca, Permission permission) {
    if (mudou)
      ouvinte.politicaMudou(this, mudanca, permission);
    return mudou;
  }

  /** A política inline. Visível só para o núcleo, que é quem resolve acesso. */
  PermissionHolder getPolicy() {
    return policy;
  }

  /** As cláusulas inline, para inspeção — é o que permite imprimir a política. */
  @Override
  public Set<Statement> getStatements() {
    return policy.getStatements();
  }

  public Set<Permission> getInlinePermissions() {
    return policy.getPermissions();
  }

  public Set<Permission> getDeniedPermissions() {
    return policy.getDeniedPermissions();
  }

  /** Esvazia a política inline (concessões e negações). */
  public void clearPermissions() {
    policy.clear();
  }

  // GROUPS

  protected void joinGroup(Group group) {
    groups.add(group);
  }

  protected void quitGroup(Group group) {
    groups.remove(group);
  }

  /**
   * Os grupos a que o usuário pertence.
   *
   * Isto e as permissões são detalhe interno da autorização e não pertencem a
   * uma resposta HTTP — mas manter esse cuidado aqui obrigaria o núcleo a
   * conhecer a biblioteca de serialização da aplicação. Quem os esconde é o
   * mixin de quem serializa; quem precisa do papel usa o DTO do UserController.
   */
  public Set<Group> getGroups() {
    return Collections.unmodifiableSet(groups);
  }

  /**
   * As políticas dos grupos também valem para o usuário. Para o motor isto é
   * só "de quem eu herdo": ele não sabe que são grupos.
   */
  @Override
  public Collection<Group> herdaDe() {
    return getGroups();
  }

  // GETTERS

  public String getId() {
    return id;
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  /**
   * Reinicia o contador de ids. Existe para os testes conseguirem rodar cada
   * cenário a partir de um estado previsível.
   */
  public static void resetIdCounter(long proximo) {
    proximoId = proximo;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o)
      return true;
    if (!(o instanceof User))
      return false;
    var other = (User) o;
    return id.equals(other.getId());
  }

  @Override
  public int hashCode() {
    return id.hashCode();
  }

  @Override
  public ResourceType getType() {
    return PrincipalResource.USUARIO;
  }
}
