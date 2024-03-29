package com.lxbluem;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.lxbluem.common.adapter.EventBusBotMessaging;
import com.lxbluem.common.adapter.EventbusEventDispatcher;
import com.lxbluem.common.domain.ports.BotMessaging;
import com.lxbluem.common.domain.ports.EventDispatcher;
import com.lxbluem.eventlogger.EventLoggerVerticle;
import com.lxbluem.filenameresolver.FilenameResolverVerticle;
import com.lxbluem.filenameresolver.adapters.InMemoryEntityStorage;
import com.lxbluem.filenameresolver.domain.interactors.FileSystemBlockingImpl;
import com.lxbluem.filenameresolver.domain.interactors.FilenameMapper;
import com.lxbluem.filenameresolver.domain.interactors.ResolvePackNameImpl;
import com.lxbluem.filenameresolver.domain.interactors.SyncStorageFromFsImpl;
import com.lxbluem.irc.DccReceiverVerticle;
import com.lxbluem.irc.NewBotVerticle;
import com.lxbluem.irc.adapters.InMemoryBotStorage;
import com.lxbluem.irc.adapters.InMemoryStateStorage;
import com.lxbluem.irc.adapters.KittehIrcBotFactory;
import com.lxbluem.irc.domain.interactors.ChangeNickNameImpl;
import com.lxbluem.irc.domain.interactors.ExitBotImpl;
import com.lxbluem.irc.domain.interactors.InitializeBotImpl;
import com.lxbluem.irc.domain.interactors.JoinMentionedChannelsImpl;
import com.lxbluem.irc.domain.interactors.LookForPackUserImpl;
import com.lxbluem.irc.domain.interactors.NoticeMessageHandlerImpl;
import com.lxbluem.irc.domain.interactors.PrepareDccTransferImpl;
import com.lxbluem.irc.domain.interactors.RegisterNickNameImpl;
import com.lxbluem.irc.domain.interactors.ScheduledTaskExecutionImpl;
import com.lxbluem.irc.domain.interactors.SkipProtectedChannelImpl;
import com.lxbluem.irc.domain.interactors.ToggleDccTransferStartedImpl;
import com.lxbluem.irc.domain.interactors.subhandlers.AlreadyDownloadedNoticeMessageHandler;
import com.lxbluem.irc.domain.interactors.subhandlers.FailureNoticeMessageHandler;
import com.lxbluem.irc.domain.interactors.subhandlers.JoinMoreChannelsNoticeMessageHandler;
import com.lxbluem.irc.domain.interactors.subhandlers.NickNameRegisteredNoticeMessageHandler;
import com.lxbluem.irc.domain.interactors.subhandlers.QueuedNoticeMessageHandler;
import com.lxbluem.irc.domain.interactors.subhandlers.RegisterNickNameNoticeMessageHandler;
import com.lxbluem.irc.domain.interactors.subhandlers.SendingYouPackNoticeMessageHandler;
import com.lxbluem.irc.domain.interactors.subhandlers.XdccSearchPackResponseMessageHandler;
import com.lxbluem.irc.domain.ports.incoming.ExitBot;
import com.lxbluem.irc.domain.ports.incoming.InitializeBot;
import com.lxbluem.irc.domain.ports.incoming.LookForPackUser;
import com.lxbluem.irc.domain.ports.incoming.NoticeMessageHandler;
import com.lxbluem.irc.domain.ports.incoming.PrepareDccTransfer;
import com.lxbluem.irc.domain.ports.incoming.RegisterNickName;
import com.lxbluem.irc.domain.ports.incoming.ToggleDccTransferStarted;
import com.lxbluem.irc.domain.ports.outgoing.BotFactory;
import com.lxbluem.irc.domain.ports.outgoing.BotStorage;
import com.lxbluem.irc.domain.ports.outgoing.NameGenerator;
import com.lxbluem.irc.domain.ports.outgoing.StateStorage;
import com.lxbluem.notification.ExternalNotificationVerticle;
import com.lxbluem.rest.RouterVerticle;
import com.lxbluem.search.SearchVerticle;
import com.lxbluem.search.adapters.ixirc.IxircSearchGateway;
import com.lxbluem.search.domain.interactors.ListMatchingPacksImpl;
import com.lxbluem.search.domain.ports.ListMatchingPacks;
import com.lxbluem.search.domain.ports.SearchGateway;
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
import io.vertx.rxjava.core.file.FileSystem;
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
        EventDispatcher eventDispatcher = new EventbusEventDispatcher(eventBus, clock);

        Verticle stateVerticle = getStateVerticle(clock);
        Verticle botVerticle = getBotVerticle(botMessaging, eventDispatcher);
        Verticle searchVerticle = getSearchVerticle(vertx);
        Verticle filenameResolverVerticle = getFilenameResolverVerticle(FileSystem.newInstance(vertx.fileSystem()));
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

    private static Verticle getFilenameResolverVerticle(FileSystem vertxFileSystem) {
        FilenameMapper filenameMapper = new FilenameMapper();
        FileSystemBlockingImpl fileSystem = new FileSystemBlockingImpl(vertxFileSystem);
        InMemoryEntityStorage storage = new InMemoryEntityStorage();
        String downloadsPath = "downloads";
        ResolvePackNameImpl resolvePackName = new ResolvePackNameImpl(filenameMapper, fileSystem, storage, downloadsPath);
        return new FilenameResolverVerticle(resolvePackName, new SyncStorageFromFsImpl(fileSystem, storage, downloadsPath));
    }


    private static Verticle getSearchVerticle(Vertx vertx) {
        SearchGateway searchGateway = new IxircSearchGateway(vertx);
//        SearchGateway searchGateway = new SunXDccSearchGateway(vertx);
        ListMatchingPacks listMatchingPacks = new ListMatchingPacksImpl(searchGateway);
        return new SearchVerticle(listMatchingPacks);
    }

    private static Verticle getBotVerticle(BotMessaging botMessaging, EventDispatcher eventDispatcher) {
        BotStorage botStorage = new InMemoryBotStorage();
        StateStorage stateStorage = new InMemoryStateStorage();
        ScheduledTaskExecutionImpl scheduledTaskExecution = new ScheduledTaskExecutionImpl();
        ExitBot exitBot = new ExitBotImpl(botStorage, stateStorage, eventDispatcher);

        NoticeMessageHandler noticeMessageHandler = new NoticeMessageHandlerImpl(eventDispatcher);
        noticeMessageHandler.registerMessageHandler(new FailureNoticeMessageHandler(exitBot, eventDispatcher));
        noticeMessageHandler.registerMessageHandler(new JoinMoreChannelsNoticeMessageHandler(botStorage, stateStorage));
        noticeMessageHandler.registerMessageHandler(new NickNameRegisteredNoticeMessageHandler(stateStorage));
        noticeMessageHandler.registerMessageHandler(new QueuedNoticeMessageHandler(eventDispatcher));
        noticeMessageHandler.registerMessageHandler(new RegisterNickNameNoticeMessageHandler(botStorage, stateStorage));
        noticeMessageHandler.registerMessageHandler(new XdccSearchPackResponseMessageHandler(botStorage, stateStorage, eventDispatcher));
        noticeMessageHandler.registerMessageHandler(new SendingYouPackNoticeMessageHandler(botStorage, stateStorage));
        noticeMessageHandler.registerMessageHandler(new AlreadyDownloadedNoticeMessageHandler(botStorage, stateStorage, eventDispatcher, scheduledTaskExecution));

        PrepareDccTransfer prepareDccTransfer = new PrepareDccTransferImpl(botStorage, stateStorage, botMessaging, exitBot);
        LookForPackUser lookForPackUser = new LookForPackUserImpl(stateStorage, exitBot, eventDispatcher);
        JoinMentionedChannelsImpl joinMentionedChannels = new JoinMentionedChannelsImpl(botStorage, stateStorage);
        RegisterNickName registerNickName = new RegisterNickNameImpl(botStorage);
        NameGenerator nameGenerator = new NameGenerator.RandomNameGenerator();
        ChangeNickNameImpl changeNickName = new ChangeNickNameImpl(botStorage, nameGenerator, eventDispatcher);
        SkipProtectedChannelImpl skipProtectedChannel = new SkipProtectedChannelImpl(stateStorage);
        BotFactory botFactory = new KittehIrcBotFactory(
                exitBot,
                noticeMessageHandler,
                prepareDccTransfer,
                lookForPackUser,
                joinMentionedChannels,
                registerNickName,
                changeNickName,
                skipProtectedChannel
        );
        InitializeBot initializeBot = new InitializeBotImpl(
                botStorage,
                stateStorage,
                eventDispatcher,
                nameGenerator,
                botFactory
        );
        ToggleDccTransferStarted toggleDccTransferStarted = new ToggleDccTransferStartedImpl(stateStorage);
        return new NewBotVerticle(initializeBot, exitBot, toggleDccTransferStarted);
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
