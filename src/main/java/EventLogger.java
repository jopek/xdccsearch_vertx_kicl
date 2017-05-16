import io.vertx.core.AbstractVerticle;
import io.vertx.core.eventbus.EventBus;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;

import static io.vertx.core.http.HttpMethod.GET;

public class EventLogger extends AbstractVerticle {

    @Override
    public void start() throws Exception {
        EventBus eventBus = vertx.eventBus();
        eventBus.consumer(Bot.ADDRESS, handler ->
                System.out.printf("_ %s\n", handler.body())
        );

        Router router = Router.router(vertx);
        router.route(GET, "/logs").handler(this::getLogs);
        vertx.createHttpServer()
                .requestHandler(router::accept)
                .listen(8080);
    }

    private void getLogs(RoutingContext routingContext) {
        routingContext.response().end("boom EventLogger " + deploymentID());
    }
}
