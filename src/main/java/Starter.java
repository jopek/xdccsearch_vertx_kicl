import io.vertx.core.Vertx;
import irc.Bot;
import search.Search;

public class Starter {
    public static void main(String[] args) {
        Vertx vertx = Vertx.vertx();

        String name;
        vertx.deployVerticle(Bot.class.getName(), event -> System.out.println(Bot.class.getName() + " deployed"));

        vertx.deployVerticle(Search.class.getName(), event -> System.out.println(Search.class.getName() + " deployed"));

        vertx.deployVerticle(EventLogger.class.getName(), event -> System.out.println(EventLogger.class.getName() + " deployed"));
    }
}
