package poo;

import io.javalin.Javalin;
import poo.api.AtividadeController;
import poo.api.ComentarioController;
import poo.api.PostController;
import poo.api.TurmaController;

public class Main {
    public static void main(String[] args) {
        Javalin app = Javalin.create(config -> {
            config.bundledPlugins.enableCors(cors -> {
                cors.addRule(it -> {
                    it.anyHost(); // Habilita acesso de qualquer origem (CORS)
                });
            });
        }).start(7000);

        TurmaController.register(app);
        PostController.register(app);
        AtividadeController.register(app);
        ComentarioController.register(app);
    }
}
