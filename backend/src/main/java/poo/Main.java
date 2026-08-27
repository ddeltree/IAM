package poo;

import io.javalin.Javalin;
import poo.api.AtividadeController;
import poo.api.ComentarioController;
import poo.api.ParticipantesController;
import poo.api.PostController;
import poo.api.TurmaController;
import poo.api.UserController;
import poo.api.exceptions.UnauthorizedException;
import poo.api.exceptions.ForbiddenException;
import poo.api.exceptions.NotFoundException;
import poo.classroom.Comentario;
import poo.classroom.Publicacao;
import poo.classroom.Turma;
import poo.iam.SecurityContext;
import poo.iam.User;

public class Main {
    public static void main(String[] args) {
        var app = createApp();
        app.start(7000);
    }

    public static Javalin createApp() {
        var app = Javalin.create(config -> {
            config.bundledPlugins.enableCors(cors -> {
                cors.addRule(it -> {
                    it.anyHost(); // Habilita acesso de qualquer origem (CORS)
                });
            });
        });

        app.exception(NotFoundException.class, (e, ctx) -> {
            ctx.status(NotFoundException.STATUS_CODE).result(e.getMessage());
        });
        app.exception(ForbiddenException.class, (e, ctx) -> {
            ctx.status(ForbiddenException.STATUS_CODE).result(e.getMessage());
        });
        app.exception(UnauthorizedException.class, (e, ctx) -> {
            ctx.status(UnauthorizedException.STATUS_CODE).result(e.getMessage());
        });

        SecurityContext.getInstance();
        TurmaController.register(app);
        PostController.register(app);
        AtividadeController.register(app);
        ComentarioController.register(app);
        UserController.register(app);
        ParticipantesController.register(app);

        return app;
    }

    /**
     * Devolve a aplicação ao estado inicial: repositórios vazios, contadores de
     * id reiniciados e permissões padrão restauradas.
     *
     * Todo o estado vive em campos estáticos, então os cenários de teste
     * compartilham a mesma JVM — sem este reset um teste enxerga os usuários e
     * ids criados pelo anterior.
     */
    public static void resetState() {
        var auth = SecurityContext.getInstance();
        auth.reset();

        UserController.reset();
        TurmaController.reset();
        PostController.reset();
        AtividadeController.reset();
        ComentarioController.reset();

        // o ADMIN é criado uma única vez, junto com o SecurityContext, e mantém o id
        User.resetIdCounter(Long.parseLong(auth.getAdmin().getId()) + 1);
        Turma.resetIdCounter();
        Publicacao.resetIdCounter();
        Comentario.resetIdCounter();
    }
}
