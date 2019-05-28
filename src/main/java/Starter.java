import com.fasterxml.jackson.databind.DeserializationFeature;
import com.lxbluem.EventLogger;
import com.lxbluem.RouterVerticle;
import com.lxbluem.filesystem.FilenameResolverVerticle;
import com.lxbluem.irc.BotVerticle;
import com.lxbluem.irc.DccReceiverVerticle;
import com.lxbluem.notification.ExternalNotificationVerticle;
import com.lxbluem.search.SearchVerticle;
import com.lxbluem.state.StateVerticle;
import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.json.Json;
import io.vertx.core.json.JsonObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;

public class Starter {
    private static final Logger LOG = LoggerFactory.getLogger(Starter.class);

    public static void main(String[] args) throws InterruptedException {
        LOG.info("starting");
        Json.mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

        Vertx vertx = Vertx.vertx();
        deploy(vertx, EventLogger.class.getName());
        deploy(vertx, DccReceiverVerticle.class.getName());
//        deploy(vertx, ExternalNotificationVerticle.class.getName());
        deploy(vertx, FilenameResolverVerticle.class.getName());

        deploy(vertx, RouterVerticle.class.getName(), event -> {
            logDeployment(RouterVerticle.class.getName(), event);
            deploy(vertx, StateVerticle.class.getName());
            deploy(vertx, SearchVerticle.class.getName());
            deploy(vertx, BotVerticle.class.getName());
        });

//        vertx.periodicStream(1000).handler(i -> vertx.eventBus().publish("time", new JsonObject().put("time", Instant.now().toString())));
//        vertx.periodicStream(1000).handler(i -> vertx.eventBus().publish("time1", new JsonObject().put("time1", Instant.now().toString())));
//        vertx.periodicStream(1100).handler(i -> vertx.eventBus().publish("time2", new JsonObject().put("time2", Instant.now().toString())));
    }

    private static void deploy(Vertx vertx, String verticleClassname) {
        vertx.deployVerticle(verticleClassname, event -> logDeployment(verticleClassname, event));
    }

    private static void deploy(Vertx vertx, String verticleClassname, Handler<AsyncResult<String>> asyncResultHandler) {
        vertx.deployVerticle(verticleClassname, asyncResultHandler);
    }

    private static void logDeployment(String name, AsyncResult<String> event) {
        if (event.succeeded())
            LOG.info("deployed {} with id {}", name, event.result());
        else {
            LOG.error("deployed {} with id {} {}", name, event.result(), " - FAILED! : " + event.cause().getMessage());
            event.cause().printStackTrace();
        }
    }
}
