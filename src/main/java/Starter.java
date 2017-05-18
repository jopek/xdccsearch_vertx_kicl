import io.vertx.core.Vertx;
import irc.Bot;
import search.Search;

public class Starter {
    public static void main(String[] args) {
        Vertx vertx = Vertx.vertx();
        vertx.deployVerticle(Bot.class.getName());
        vertx.deployVerticle(Search.class.getName());
        vertx.deployVerticle(EventLogger.class.getName());
    }
}
