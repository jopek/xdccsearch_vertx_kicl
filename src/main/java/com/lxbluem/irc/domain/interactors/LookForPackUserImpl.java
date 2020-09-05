package com.lxbluem.irc.domain.interactors;

import com.lxbluem.common.domain.events.BotFailedEvent;
import com.lxbluem.common.domain.ports.EventDispatcher;
import com.lxbluem.irc.domain.model.request.LookForPackUserCommand;
import com.lxbluem.irc.domain.model.request.ManualExitCommand;
import com.lxbluem.irc.domain.ports.incoming.ExitBot;
import com.lxbluem.irc.domain.ports.incoming.LookForPackUser;
import com.lxbluem.irc.domain.ports.outgoing.BotStateStorage;

import java.time.Clock;
import java.time.Instant;
import java.util.List;

import static java.lang.String.format;

public class LookForPackUserImpl implements LookForPackUser {
    private final BotStateStorage stateStorage;
    private final ExitBot exitBot;
    private final EventDispatcher eventDispatcher;
    private final Clock clock;

    public LookForPackUserImpl(BotStateStorage stateStorage, ExitBot exitBot, EventDispatcher eventDispatcher, Clock clock) {
        this.stateStorage = stateStorage;
        this.exitBot = exitBot;
        this.eventDispatcher = eventDispatcher;
        this.clock = clock;
    }

    @Override
    public void handle(LookForPackUserCommand command) {
        String botNickName = command.getBotNickName();
        String channelName = command.getChannelName();
        List<String> usersInChannel = command.getUsersInChannel();

        stateStorage.get(botNickName).ifPresent(botState -> {
            botState.channelNickList(channelName, usersInChannel);
            if (!botState.hasSeenRemoteUser()) {
                String remoteUser = botState.getPack().getNickName();
                final String message = format("bot %s not in channel %s", remoteUser, channelName);
                BotFailedEvent failedEvent = new BotFailedEvent(botNickName, nowEpochMillis(), message);
                eventDispatcher.dispatch(failedEvent);
                exitBot.handle(new ManualExitCommand(failedEvent.getBot(), failedEvent.getMessage()));
            }
        });
    }

    private long nowEpochMillis() {
        return Instant.now(clock).toEpochMilli();
    }
}
