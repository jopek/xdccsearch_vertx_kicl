package com.lxbluem.irc.domain.interactors;

import com.lxbluem.common.domain.events.BotFailedEvent;
import com.lxbluem.common.domain.ports.EventDispatcher;
import com.lxbluem.irc.domain.model.request.LookForPackUserCommand;
import com.lxbluem.irc.domain.model.request.ReasonedExitCommand;
import com.lxbluem.irc.domain.ports.incoming.ExitBot;
import com.lxbluem.irc.domain.ports.incoming.LookForPackUser;
import com.lxbluem.irc.domain.ports.outgoing.StateStorage;

import java.util.List;

import static java.lang.String.format;

public class LookForPackUserImpl implements LookForPackUser {
    private final StateStorage stateStorage;
    private final ExitBot exitBot;
    private final EventDispatcher eventDispatcher;

    public LookForPackUserImpl(StateStorage stateStorage, ExitBot exitBot, EventDispatcher eventDispatcher) {
        this.stateStorage = stateStorage;
        this.exitBot = exitBot;
        this.eventDispatcher = eventDispatcher;
    }

    @Override
    public void handle(LookForPackUserCommand command) {
        String botNickName = command.getBotNickName();
        String channelName = command.getChannelName();
        List<String> usersInChannel = command.getUsersInChannel();

        stateStorage.get(botNickName).ifPresent(state -> {
            state.channelNickList(channelName, usersInChannel);
            if (!state.isRemoteUserSeen()) {
                String remoteUser = state.getPack().getNickName();
                final String message = format("bot %s not in channel %s", remoteUser, channelName);
                BotFailedEvent failedEvent = new BotFailedEvent(botNickName, message);
                eventDispatcher.dispatch(failedEvent);
                exitBot.handle(new ReasonedExitCommand(failedEvent.getBot(), failedEvent.getMessage()));
            }
        });
    }
}
