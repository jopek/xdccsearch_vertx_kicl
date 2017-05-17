import io.vertx.core.Vertx;
import irc.Bot;

public class Starter {
    public static void main(String[] args) {
        Vertx vertx = Vertx.vertx();
        vertx.deployVerticle(Bot.class.getName());
        vertx.deployVerticle(EventLogger.class.getName());
    }
}
