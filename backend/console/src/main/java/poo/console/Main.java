package poo.console;

import com.fasterxml.jackson.databind.ObjectMapper;

import io.javalin.Javalin;
import io.javalin.json.JavalinJackson;
import poo.console.api.CenarioController;
import poo.console.api.ConsultasController;
import poo.console.api.IamMixins;
import poo.console.api.PoliticasController;
import poo.console.api.PrincipaisController;
import poo.console.api.RecursosController;
import poo.console.api.VocabularioController;

/**
 * O console de IAM, na porta 7001.
 *
 * Segunda aplicação do {@code iam-core}, e a que o exercita por inteiro: aqui o
 * vocabulário é criado pela tela, e não declarado em enums. O classroom, na
 * 7000, continua existindo e não sabe que isto existe.
 *
 * Não há autenticação. O console é uma ferramenta para montar e inspecionar
 * políticas — quem responde por elas é escolhido num seletor, não por login.
 * Governá-lo com o próprio núcleo seria a demonstração mais forte que existe e
 * também o jeito mais fácil de se trancar fora dele durante uma apresentação.
 */
public class Main {

  public static void main(String[] args) {
    createApp().start(7001);
  }

  public static Javalin createApp() {
    var app = Javalin.create(config -> {
      config.jsonMapper(new JavalinJackson(IamMixins.aplicar(new ObjectMapper()), false));
      config.bundledPlugins.enableCors(cors -> cors.addRule(it -> it.anyHost()));
    });

    app.exception(IllegalArgumentException.class, (e, ctx) -> ctx.status(400).result(msg(e)));
    app.exception(IllegalStateException.class, (e, ctx) -> ctx.status(409).result(msg(e)));
    app.exception(ClassCastException.class,
        (e, ctx) -> ctx.status(400).result("Formato inesperado no corpo da requisição"));

    CenarioController.register(app);
    PrincipaisController.register(app);
    PoliticasController.register(app);
    RecursosController.register(app);
    VocabularioController.register(app);
    ConsultasController.register(app);
    return app;
  }

  private static String msg(Exception e) {
    return e.getMessage() == null ? e.getClass().getSimpleName() : e.getMessage();
  }

  /** Devolve o console ao cenário semente. Usado pelos testes e pela tela. */
  public static void resetState() {
    Cenario.reiniciar();
  }
}
