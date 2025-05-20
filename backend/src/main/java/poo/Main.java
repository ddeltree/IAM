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
import poo.iam.SecurityContext;

public class Main {
    public static void main(String[] args) {
        Javalin app = Javalin.create(config -> {
            config.bundledPlugins.enableCors(cors -> {
                cors.addRule(it -> {
                    it.anyHost(); // Habilita acesso de qualquer origem (CORS)
                });
            });
        }).start(7000);

        app.exception(NotFoundException.class, (e, ctx) -> {
            ctx.status(404).result(e.getMessage());
        });
        app.exception(ForbiddenException.class, (e, ctx) -> {
            ctx.status(403).result(e.getMessage());
        });
        app.exception(UnauthorizedException.class, (e, ctx) -> {
            ctx.status(401).result(e.getMessage());
        });

        SecurityContext.getInstance();
        TurmaController.register(app);
        PostController.register(app);
        AtividadeController.register(app);
        ComentarioController.register(app);
        UserController.register(app);
        ParticipantesController.register(app);
    }
}
