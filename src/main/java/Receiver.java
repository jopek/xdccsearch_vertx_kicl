import io.vertx.core.AbstractVerticle;
import io.vertx.core.eventbus.Message;

public class Receiver extends AbstractVerticle {

    @Override
    public void start() {
        vertx.eventBus()
                .consumer("loop", message -> {
                    log(message);
                    message.reply("pong!");
                });

        vertx.eventBus()
                .consumer("pub", message -> {
                    log(message);
                    message.reply("pong!");
                });
    }

    private void log(Message<Object> message) {
        System.out.printf(
                "receiver %s '%s' received message '%s' to '%s'  sending reply\n",
                deploymentID(),
                message.replyAddress(),
                message.body(),
                message.address()
        );
    }

}
