package com.lxbluem.irc.domain.interactors;

import com.lxbluem.common.domain.events.BotRenamedEvent;
import com.lxbluem.common.domain.ports.EventDispatcher;
import com.lxbluem.irc.domain.model.request.ChangeNickNameCommand;
import com.lxbluem.irc.domain.ports.incoming.ChangeNickName;
import com.lxbluem.irc.domain.ports.outgoing.BotStorage;
import com.lxbluem.irc.domain.ports.outgoing.NameGenerator;

import java.time.Clock;
import java.time.Instant;

public class ChangeNickNameImpl implements ChangeNickName {
    private final BotStorage botStorage;
    private final NameGenerator nameGenerator;
    private final EventDispatcher eventDispatcher;
    private final Clock clock;

    public ChangeNickNameImpl(
            BotStorage botStorage,
            NameGenerator nameGenerator,
            EventDispatcher eventDispatcher,
            Clock clock) {
        this.botStorage = botStorage;
        this.nameGenerator = nameGenerator;
        this.eventDispatcher = eventDispatcher;
        this.clock = clock;
    }

    @Override
    public void handle(ChangeNickNameCommand command) {
        String botNickName = command.getBotNickName();
        String serverMessages = command.getServerMessages();
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

    private long nowEpochMillis() {
        return Instant.now(clock).toEpochMilli();
    }
}
