package com.lxbluem.irc.domain;

import com.lxbluem.common.domain.events.BotRenamedEvent;
import com.lxbluem.common.domain.ports.EventDispatcher;
import com.lxbluem.irc.domain.ports.outgoing.BotStateStorage;
import com.lxbluem.irc.domain.ports.outgoing.BotStorage;
import com.lxbluem.irc.domain.ports.outgoing.NameGenerator;

import java.time.Clock;
import java.time.Instant;

public class BotService {
    private final BotStorage botStorage;
    private final BotStateStorage stateStorage;
    private final EventDispatcher eventDispatcher;
    private final Clock clock;
    private final NameGenerator nameGenerator;

    public BotService(
            BotStorage botStorage,
            BotStateStorage stateStorage,
            EventDispatcher eventDispatcher,
            Clock clock,
            NameGenerator nameGenerator
    ) {
        this.botStorage = botStorage;
        this.stateStorage = stateStorage;
        this.eventDispatcher = eventDispatcher;
        this.clock = clock;
        this.nameGenerator = nameGenerator;
    }

    public void changeNick(String botNickName, String serverMessages) {
        botStorage.get(botNickName).ifPresent(bot -> {
            String newBotNickName = nameGenerator.getNick();
            bot.changeNickname(newBotNickName);
            BotRenamedEvent renameMessage = BotRenamedEvent.builder()
                    .attemptedBotName(botNickName)
                    .renameto(newBotNickName)
                    .serverMessages(serverMessages)
                    .timestamp(nowEpochMillis())
                    .build();
            eventDispatcher.dispatch(renameMessage);
        });
    }

    public void channelRequiresAccountRegistry(String botNickName, String channelName, String message) {
        stateStorage.get(botNickName).ifPresent(botState -> {
            if (message.toLowerCase().contains("registered account to join")) {
                botState.removeReferencedChannel(channelName);
            }
        });
    }

    private long nowEpochMillis() {
        return Instant.now(clock).toEpochMilli();
    }
}
