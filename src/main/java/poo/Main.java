package poo;

import poo.iam.*;
import poo.iam.resources.*;
import io.javalin.Javalin;

public class Main {
    public static void main(String[] args) {
        // Javalin app = Javalin.create(config -> {
        // config.bundledPlugins.enableCors(cors -> {
        // cors.addRule(it -> {
        // it.anyHost(); // Habilita acesso de qualquer origem (CORS)
        // });
        // });
        // }).start(7000);

        // app.get("/hello", ctx -> {
        // ctx.result("Olá, mundo!");
        // });
        exemplo();
    }

    public static void exemplo() {
        // CRIANDO RECURSOS -- Document ou qualquer um que herde de Resource
        var documento = new Document();
        var documento2 = ResourceFactory.createResource(ResourceTypes.DOCUMENT); // equivalente ao de cima

        // CRIANDO PERMISSÕES -- permissão é uma ação sobre um recurso
        var poderVerDocumento = new Permission(Action.VIEW, documento);
        var poderExcluirDocumento = new Permission(Action.DELETE, documento2);

        // CRIANDO USUÁRIOS
        var usuarioComum = new User();
        var usuarioAdmin = new User("admin1"); // admin1 é só um id desnecessário, para prints
        // o usuário administrador é aquele que recebe permissões de admin

        // CRIANDO GRUPOS
        // Um usuário não precisa estar em um grupo
        // Group grupoComum = new Group("Associação ordinária dos comuns");
        Group grupoAdmin = new Group("Administradores");

        // DANDO PERMISSÕES A USUÁRIOS E GRUPOS
        usuarioComum.grantPermission(poderVerDocumento);
        grupoAdmin.grantPermission(poderVerDocumento); // o usuário admin herda a permissão do grupo
        grupoAdmin.grantPermission(poderExcluirDocumento); // apenas admins podem excluir documentos
        MembershipManager.link(usuarioAdmin, grupoAdmin); // adicionar o admin ao grupo dos admins

        // CHECANDO PERMISSÕES
        var podeComumVerDocumento = PermissionService.hasPermission(usuarioComum, poderVerDocumento);
        var podeComumExcluirDocumento = PermissionService.hasPermission(usuarioComum, Action.DELETE, documento2);
        var podeAdminVerDocumento = PermissionService.hasPermission(usuarioAdmin, Action.VIEW, documento);
        var podeAdminExcluirDocumento = PermissionService.hasPermission(usuarioAdmin, poderExcluirDocumento);

        System.out.println("Usuário COMUM sem grupo pode VER documento: " + podeComumVerDocumento);
        System.out.println("Usuário COMUM sem grupo pode EXCLUIR documento: " + podeComumExcluirDocumento);
        System.out.println("Usuário ADMIN com grupo pode VER documento: " + podeAdminVerDocumento);
        System.out.println("Usuário ADMIN com grupo pode EXCLUIR documento: " + podeAdminExcluirDocumento);
    }
}
