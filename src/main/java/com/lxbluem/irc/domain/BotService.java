package com.lxbluem.irc.domain;

import com.lxbluem.irc.domain.ports.outgoing.BotStateStorage;

import java.time.Clock;

public class BotService {
    private final BotStateStorage stateStorage;
    private final Clock clock;

    public BotService(
            BotStateStorage stateStorage,
            Clock clock
    ) {
        this.stateStorage = stateStorage;
        this.clock = clock;
    }


    public void channelRequiresAccountRegistry(String botNickName, String channelName, String message) {
        stateStorage.get(botNickName).ifPresent(botState -> {
            if (message.toLowerCase().contains("registered account to join")) {
                botState.removeReferencedChannel(channelName);
            }
        });
    }
}
