package com.lxbluem;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.lxbluem.common.adapter.EventBusBotMessaging;
import com.lxbluem.common.adapter.EventbusEventDispatcher;
import com.lxbluem.common.domain.ports.BotMessaging;
import com.lxbluem.common.domain.ports.EventDispatcher;
import com.lxbluem.eventlogger.EventLoggerVerticle;
import com.lxbluem.filesystem.FilenameResolverVerticle;
import com.lxbluem.irc.DccReceiverVerticle;
import com.lxbluem.irc.NewBotVerticle;
import com.lxbluem.irc.adapters.InMemoryBotStateStorage;
import com.lxbluem.irc.adapters.InMemoryBotStorage;
import com.lxbluem.irc.adapters.KittehIrcBotFactory;
import com.lxbluem.irc.domain.interactors.*;
import com.lxbluem.irc.domain.ports.incoming.*;
import com.lxbluem.irc.domain.ports.outgoing.BotFactory;
import com.lxbluem.irc.domain.ports.outgoing.BotStateStorage;
import com.lxbluem.irc.domain.ports.outgoing.BotStorage;
import com.lxbluem.irc.domain.ports.outgoing.NameGenerator;
import com.lxbluem.notification.ExternalNotificationVerticle;
import com.lxbluem.rest.RouterVerticle;
import com.lxbluem.search.SearchVerticle;
import com.lxbluem.state.StateVerticle;
import com.lxbluem.state.adapters.InMemoryStateRepository;
import com.lxbluem.state.domain.StateService;
import com.lxbluem.state.domain.ports.StateRepository;
import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;
import io.vertx.core.Verticle;
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

        Clock clock = Clock.systemDefaultZone();
        Vertx vertx = Vertx.vertx();
        EventBus eventBus = vertx.eventBus();
        BotMessaging botMessaging = new EventBusBotMessaging(eventBus, clock);
        EventDispatcher eventDispatcher = new EventbusEventDispatcher(eventBus);

        Verticle stateVerticle = getStateVerticle(clock);
        Verticle botVerticle = getBotVerticle(botMessaging, clock, eventDispatcher);
        Verticle searchVerticle = new SearchVerticle();
        Verticle filenameResolverVerticle = new FilenameResolverVerticle();
        Verticle receiverVerticle = new DccReceiverVerticle(botMessaging);
        Verticle eventLoggerVerticle = new EventLoggerVerticle();
        Verticle notificationVerticle = new ExternalNotificationVerticle();
        Verticle routerVerticle = new RouterVerticle();

        deployWithHook(vertx, routerVerticle, deploymentId -> {
            deploy(vertx, stateVerticle);
            deploy(vertx, searchVerticle);
            deploy(vertx, botVerticle);
        });
        deploy(vertx, eventLoggerVerticle);
        deploy(vertx, receiverVerticle);
//        deploy(vertx, notificationVerticle);
        deploy(vertx, filenameResolverVerticle);
    }

    private static Verticle getBotVerticle(BotMessaging botMessaging, Clock clock, EventDispatcher eventDispatcher) {
        BotStorage botStorage = new InMemoryBotStorage();
        BotStateStorage botStateStorage = new InMemoryBotStateStorage();
        NameGenerator nameGenerator = new NameGenerator.RandomNameGenerator();
        ExitBot exitBot = new ExitBotImpl(botStorage, botStateStorage, eventDispatcher, clock);
        NoticeMessageHandler noticeMessageHandler = new NoticeMessageHandlerImpl(botStorage, botStateStorage, eventDispatcher, clock, exitBot);
        CtcpQueryHandler ctcpQueryHandler = new CtcpQueryHandlerImpl(botStorage, botStateStorage, botMessaging);
        LookForPackUser lookForPackUser = new LookForPackUserImpl(botStateStorage, exitBot, eventDispatcher, clock);
        JoinMentionedChannelsImpl joinMentionedChannels = new JoinMentionedChannelsImpl(botStorage, botStateStorage);
        RegisterNickName registerNickName = new RegisterNickNameImpl(botStorage);
        ChangeNickNameImpl changeNickName = new ChangeNickNameImpl(botStorage, nameGenerator, eventDispatcher, clock);
        SkipProtectedChannelImpl skipProtectedChannel = new SkipProtectedChannelImpl(botStateStorage);
        BotFactory botFactory = new KittehIrcBotFactory(
                exitBot,
                noticeMessageHandler,
                ctcpQueryHandler,
                lookForPackUser,
                joinMentionedChannels,
                registerNickName,
                changeNickName,
                skipProtectedChannel
        );
        InitializeBot initializeBot = new InitializeBotImpl(
                botStorage,
                botStateStorage,
                eventDispatcher,
                clock,
                nameGenerator,
                botFactory
        );
        return new NewBotVerticle(initializeBot, exitBot);
    }

    private static Verticle getStateVerticle(Clock clock) {
        StateRepository stateRepository = new InMemoryStateRepository();
        StateService stateService = new StateService(stateRepository, clock);
        return new StateVerticle(stateService, clock);
    }

    private static void deploy(Vertx vertx, Verticle verticle) {
        vertx.deployVerticle(verticle, deploymentId ->
            logDeployment(verticle, deploymentId));
    }

    private static void deployWithHook(Vertx vertx, Verticle verticle, Handler<AsyncResult<String>> hook) {
        vertx.deployVerticle(verticle, deploymentId -> {
            hook.handle(deploymentId);
            logDeployment(verticle, deploymentId);
        });
    }

    private static void logDeployment(Verticle name, AsyncResult<String> deploymentId) {
        if (deploymentId.succeeded())
            log.info("deployed {} with id {}", name, deploymentId.result());
        else {
            log.error("deployed {} with id {} {}", name, deploymentId.result(), " - FAILED! : " + deploymentId.cause().getMessage());
            deploymentId.cause().printStackTrace();
        }
    }
}
