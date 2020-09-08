package com.lxbluem.irc.domain.interactors;

import com.lxbluem.irc.domain.model.request.SkipProtectedChannelCommand;
import com.lxbluem.irc.domain.ports.incoming.SkipProtectedChannel;
import com.lxbluem.irc.domain.ports.outgoing.StateStorage;

public class SkipProtectedChannelImpl implements SkipProtectedChannel {
    private final StateStorage stateStorage;

    public SkipProtectedChannelImpl(StateStorage stateStorage) {
        this.stateStorage = stateStorage;
    }

    @Override
    public void handle(SkipProtectedChannelCommand command) {
        String botNickName = command.getBotNickName();
        String channelName = command.getChannelName();
        String message = command.getMessage();

        stateStorage.get(botNickName).ifPresent(state -> {
            if (message.toLowerCase().contains("registered account to join")) {
                state.removeReferencedChannel(channelName);
            }
        });
    }
}
