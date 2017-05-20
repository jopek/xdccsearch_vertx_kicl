import io.vertx.core.AbstractVerticle;
import io.vertx.core.AsyncResult;
import io.vertx.core.eventbus.Message;

public class Sender extends AbstractVerticle {

    @Override
    public void start() {
        vertx.eventBus().send("loop", "ping", reply -> {
            if (reply.succeeded())
                log(reply);
        });

        vertx.eventBus().send("loop", "ping", reply -> {
            if (reply.succeeded())
                log(reply);
        });

        vertx.eventBus().send("loop", "ping", reply -> {
            if (reply.succeeded())
                log(reply);
        });

        vertx.eventBus().publish("pub", "ping");
    }

    private void log(AsyncResult<Message<Object>> reply) {
        System.out.printf(
                "sender received reply %s from %s\n",
                reply.result().body(),
                reply.result().address()
        );
    }

}
