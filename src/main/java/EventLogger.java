import io.vertx.core.AbstractVerticle;
import io.vertx.core.eventbus.EventBus;
import irc.Bot;
import search.Search;

public class EventLogger extends AbstractVerticle {

    @Override
    public void start() throws Exception {
        EventBus eventBus = vertx.eventBus();

        eventBus.consumer(Bot.ADDRESS, handler ->
                System.out.printf("_ %s\n", handler.body())
        );

        eventBus.consumer(Search.RESULTS, handler ->
                System.out.printf("_ %s\n", handler.body())
        );
    }
}
