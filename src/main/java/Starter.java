import com.fasterxml.jackson.databind.DeserializationFeature;
import com.lxbluem.EventLogger;
import com.lxbluem.RouterVerticle;
import com.lxbluem.adapter.EventBusBotMessaging;
import com.lxbluem.domain.ports.BotMessaging;
import com.lxbluem.filesystem.FilenameResolverVerticle;
import com.lxbluem.irc.*;
import com.lxbluem.irc.adapter.InMemoryBotStateStorage;
import com.lxbluem.irc.adapter.InMemoryBotStorage;
import com.lxbluem.irc.adapter.IrcBotFactory;
import com.lxbluem.irc.usecase.BotFactory;
import com.lxbluem.irc.usecase.BotService;
import com.lxbluem.irc.usecase.ports.BotStorage;
import com.lxbluem.irc.usecase.ports.DccBotStateStorage;
import com.lxbluem.search.SearchVerticle;
import com.lxbluem.state.StateService;
import com.lxbluem.state.StateVerticle;
import com.lxbluem.state.adapters.InMemoryStateRepository;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;
import io.vertx.core.json.jackson.DatabindCodec;
import io.vertx.rxjava.core.Vertx;
import io.vertx.rxjava.core.eventbus.EventBus;
import lombok.extern.slf4j.Slf4j;

import java.time.Clock;

@Slf4j
public class Starter {

    public static void main(String[] args) {
        log.info("starting");
        DatabindCodec.mapper().configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

        Vertx vertx = Vertx.vertx();
        EventBus eventBus = vertx.eventBus();
        Clock clock = Clock.systemDefaultZone();

        BotMessaging botMessaging = new EventBusBotMessaging(eventBus, clock);

        StateService stateService = new StateService(new InMemoryStateRepository(), clock);

        BotStorage botStorage = new InMemoryBotStorage();
        DccBotStateStorage dccBotStateStorage = new InMemoryBotStateStorage();
        BotFactory botFactory = new IrcBotFactory();
        NameGenerator nameGenerator = new RandomNameGenerator();
        BotService botService = new BotService(botStorage, dccBotStateStorage, botMessaging, botFactory, clock, nameGenerator);

        deploy(vertx, EventLogger.class.getName());
        deploy(vertx, new DccReceiverVerticle(botMessaging));
//        deploy(vertx, ExternalNotificationVerticle.class.getName());
        deploy(vertx, FilenameResolverVerticle.class.getName());

        deploy(vertx, RouterVerticle.class.getName(), event -> {
            logDeployment(RouterVerticle.class.getName(), event);
            deploy(vertx, new StateVerticle(stateService));
            deploy(vertx, SearchVerticle.class.getName());
//            deploy(vertx, new BotVerticle(botMessaging));
            deploy(vertx, new NewBotVerticle(botService));
        });

//        vertx.periodicStream(1000).handler(i -> vertx.eventBus().publish("time", new JsonObject().put("time", Instant.now().toString())));
//        vertx.periodicStream(1000).handler(i -> vertx.eventBus().publish("time1", new JsonObject().put("time1", Instant.now().toString())));
//        vertx.periodicStream(1100).handler(i -> vertx.eventBus().publish("time2", new JsonObject().put("time2", Instant.now().toString())));
    }

    private static void deploy(Vertx vertx, String verticleClassname) {
        vertx.deployVerticle(verticleClassname, event -> logDeployment(verticleClassname, event));
    }

    private static void deploy(Vertx vertx, AbstractVerticle verticle) {
        vertx.getDelegate().deployVerticle(verticle, event -> logDeployment(verticle.getClass().getName(), event));
    }

    private static void deploy(Vertx vertx, String verticleClassname, Handler<AsyncResult<String>> asyncResultHandler) {
        vertx.deployVerticle(verticleClassname, asyncResultHandler);
    }

    private static void logDeployment(String name, AsyncResult<String> event) {
        if (event.succeeded())
            log.info("deployed {} with id {}", name, event.result());
        else {
            log.error("deployed {} with id {} {}", name, event.result(), " - FAILED! : " + event.cause().getMessage());
            event.cause().printStackTrace();
        }
    }
}
