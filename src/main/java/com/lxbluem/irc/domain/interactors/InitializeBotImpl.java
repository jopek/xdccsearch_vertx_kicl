package com.lxbluem.irc.domain.interactors;

import com.lxbluem.common.domain.Pack;
import com.lxbluem.common.domain.events.BotDccPackRequestedEvent;
import com.lxbluem.common.domain.events.BotInitializedEvent;
import com.lxbluem.common.domain.ports.EventDispatcher;
import com.lxbluem.irc.domain.model.State;
import com.lxbluem.irc.domain.model.request.BotConnectionDetails;
import com.lxbluem.irc.domain.model.request.InitializeBotCommand;
import com.lxbluem.irc.domain.ports.incoming.InitializeBot;
import com.lxbluem.irc.domain.ports.outgoing.BotFactory;
import com.lxbluem.irc.domain.ports.outgoing.BotStorage;
import com.lxbluem.irc.domain.ports.outgoing.IrcBot;
import com.lxbluem.irc.domain.ports.outgoing.NameGenerator;
import com.lxbluem.irc.domain.ports.outgoing.StateStorage;

import java.util.function.Consumer;

public class InitializeBotImpl implements InitializeBot {
    private final NameGenerator nameGenerator;
    private final BotFactory botFactory;
    private final BotStorage botStorage;
    private final StateStorage stateStorage;
    private final EventDispatcher eventDispatcher;

    public InitializeBotImpl(
            BotStorage botStorage,
            StateStorage stateStorage,
            EventDispatcher eventDispatcher,
            NameGenerator nameGenerator,
            BotFactory botFactory
    ) {
        this.nameGenerator = nameGenerator;
        this.botFactory = botFactory;
        this.botStorage = botStorage;
        this.stateStorage = stateStorage;
        this.eventDispatcher = eventDispatcher;
    }

    @Override
    public String handle(InitializeBotCommand initializeBotCommand) {
        Pack pack = initializeBotCommand.getPack();
        String botNickName = nameGenerator.getNick();

        IrcBot bot = botFactory.createNewInstance();
        botStorage.save(botNickName, bot);

        BotConnectionDetails botConnectionDetails = connectionDetailsFromPack(pack, botNickName);
        bot.connect(botConnectionDetails);
        bot.joinChannel(pack.getChannelName());

        Runnable requestHook = dccRequestHook(botNickName, pack);
        State state = new State(pack, requestHook);
        stateStorage.save(botNickName, state);

        eventDispatcher.dispatch(new BotInitializedEvent(botNickName, pack));

        return botNickName;
    }

    private BotConnectionDetails connectionDetailsFromPack(Pack pack, String botNickName) {
        return new BotConnectionDetails(
                pack.getServerHostName(),
                pack.getServerPort(),
                botNickName,
                "name_" + botNickName,
                "user_" + botNickName,
                "realname_" + botNickName
        );
    }

    private Runnable dccRequestHook(String botNickName, Pack pack) {
        Consumer<IrcBot> botRequestFn = bot -> {
            bot.requestDccPack(pack.getNickName(), pack.getPackNumber());

            String noticeMessage = String.format("requesting pack #%s from %s", pack.getPackNumber(), pack.getNickName());
            BotDccPackRequestedEvent event = new BotDccPackRequestedEvent(botNickName, noticeMessage, pack.getNickName(), pack.getPackNumber());
            eventDispatcher.dispatch(event);
        };

        return () -> botStorage.get(botNickName)
                .ifPresent(botRequestFn);
    }

}
