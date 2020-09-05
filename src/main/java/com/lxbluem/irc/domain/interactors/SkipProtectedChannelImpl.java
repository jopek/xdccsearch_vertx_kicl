package com.lxbluem.irc.domain.interactors;

import com.lxbluem.irc.domain.model.request.SkipProtectedChannelCommand;
import com.lxbluem.irc.domain.ports.incoming.SkipProtectedChannel;
import com.lxbluem.irc.domain.ports.outgoing.BotStateStorage;

public class SkipProtectedChannelImpl implements SkipProtectedChannel {
    private final BotStateStorage stateStorage;

    public SkipProtectedChannelImpl(BotStateStorage stateStorage) {
        this.stateStorage = stateStorage;
    }

    @Override
    public void handle(SkipProtectedChannelCommand command) {
        String botNickName = command.getBotNickName();
        String channelName = command.getChannelName();
        String message = command.getMessage();

        stateStorage.get(botNickName).ifPresent(botState -> {
            if (message.toLowerCase().contains("registered account to join")) {
                botState.removeReferencedChannel(channelName);
            }
        });
    }
}
