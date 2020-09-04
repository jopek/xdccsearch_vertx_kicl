package com.lxbluem.irc.domain.interactors;

import com.lxbluem.common.domain.Pack;
import com.lxbluem.common.domain.events.BotDccPackRequestedEvent;
import com.lxbluem.common.domain.events.BotInitializedEvent;
import com.lxbluem.common.domain.ports.EventDispatcher;
import com.lxbluem.irc.domain.BotService;
import com.lxbluem.irc.domain.model.BotState;
import com.lxbluem.irc.domain.model.request.BotConnectionDetails;
import com.lxbluem.irc.domain.model.request.InitializeBotCommand;
import com.lxbluem.irc.domain.ports.incoming.InitializeBot;
import com.lxbluem.irc.domain.ports.outgoing.*;

import java.time.Clock;
import java.time.Instant;
import java.util.function.Consumer;

public class InitializeBotImpl implements InitializeBot {
    private final NameGenerator nameGenerator;
    private final BotFactory botFactory;
    private final BotStorage botStorage;
    private final BotStateStorage stateStorage;
    private final EventDispatcher eventDispatcher;
    private final Clock clock;
    private final BotService botService;

    public InitializeBotImpl(
            BotStorage botStorage,
            BotStateStorage stateStorage,
            EventDispatcher eventDispatcher,
            Clock clock,
            NameGenerator nameGenerator,
            BotFactory botFactory,
            BotService botService
    ) {
        this.nameGenerator = nameGenerator;
        this.botFactory = botFactory;
        this.botStorage = botStorage;
        this.stateStorage = stateStorage;
        this.eventDispatcher = eventDispatcher;
        this.clock = clock;
        this.botService = botService;
    }

    @Override
    public String handle(InitializeBotCommand initializeBotCommand) {
        Pack pack = initializeBotCommand.getPack();
        String botNickName = nameGenerator.getNick();

        IrcBot bot = botFactory.createNewInstance(botService);
        botStorage.save(botNickName, bot);

        BotConnectionDetails botConnectionDetails = connectionDetailsFromPack(pack, botNickName);
        bot.connect(botConnectionDetails);
        bot.joinChannel(pack.getChannelName());

        Runnable requestHook = dccRequestHook(botNickName, pack);
        BotState botState = new BotState(pack, requestHook);
        stateStorage.save(botNickName, botState);

        eventDispatcher.dispatch(new BotInitializedEvent(botNickName, nowEpochMillis(), pack));

        return botNickName;
    }

    private BotConnectionDetails connectionDetailsFromPack(Pack pack, String botNickName) {
        return BotConnectionDetails.builder()
                .botNick(botNickName)
                .name("name_" + botNickName)
                .user("user_" + botNickName)
                .realName("realname_" + botNickName)
                .serverHostName(pack.getServerHostName())
                .serverPort(pack.getServerPort())
                .build();
    }

    private Runnable dccRequestHook(String botNickName, Pack pack) {
        Consumer<IrcBot> botRequestFn = bot -> {
            bot.requestDccPack(pack.getNickName(), pack.getPackNumber());

            String noticeMessage = String.format("requesting pack #%s from %s", pack.getPackNumber(), pack.getNickName());
            BotDccPackRequestedEvent event = new BotDccPackRequestedEvent(botNickName, nowEpochMillis(), noticeMessage, pack.getNickName(), pack.getPackNumber());
            eventDispatcher.dispatch(event);
        };

        return () -> botStorage.get(botNickName)
                .ifPresent(botRequestFn);
    }

    private long nowEpochMillis() {
        return Instant.now(clock).toEpochMilli();
    }

}
