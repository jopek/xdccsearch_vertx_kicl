package com.lxbluem;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.lxbluem.common.adapter.EventBusBotMessaging;
import com.lxbluem.common.domain.ports.BotMessaging;
import com.lxbluem.eventlogger.EventLoggerVerticle;
import com.lxbluem.filesystem.FilenameResolverVerticle;
import com.lxbluem.irc.DccReceiverVerticle;
import com.lxbluem.irc.NewBotVerticle;
import com.lxbluem.irc.adapters.InMemoryBotStateStorage;
import com.lxbluem.irc.adapters.InMemoryBotStorage;
import com.lxbluem.irc.adapters.IrcBotFactory;
import com.lxbluem.irc.domain.BotService;
import com.lxbluem.irc.domain.ports.BotFactory;
import com.lxbluem.irc.domain.ports.BotStorage;
import com.lxbluem.irc.domain.ports.DccBotStateStorage;
import com.lxbluem.irc.domain.ports.NameGenerator;
import com.lxbluem.rest.RouterVerticle;
import com.lxbluem.search.SearchVerticle;
import com.lxbluem.state.StateVerticle;
import com.lxbluem.state.adapters.InMemoryStateRepository;
import com.lxbluem.state.domain.StateService;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.json.jackson.DatabindCodec;
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
        NameGenerator nameGenerator = new NameGenerator.RandomNameGenerator();
        BotService botService = new BotService(botStorage, dccBotStateStorage, botMessaging, botFactory, clock, nameGenerator);

        deploy(vertx, EventLoggerVerticle.class.getName());
        deploy(vertx, new DccReceiverVerticle(botMessaging));
//        deploy(vertx, ExternalNotificationVerticle.class.getName());
        deploy(vertx, FilenameResolverVerticle.class.getName());

        deploy(vertx, RouterVerticle.class.getName(), event -> {
            logDeployment(RouterVerticle.class.getName(), event);
            deploy(vertx, new StateVerticle(stateService, clock));
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
        vertx.deployVerticle(verticle, event -> logDeployment(verticle.getClass().getName(), event));
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
