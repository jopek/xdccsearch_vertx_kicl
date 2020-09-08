package com.lxbluem.irc.domain.interactors;

import com.lxbluem.common.domain.events.BotExitedEvent;
import com.lxbluem.common.domain.ports.EventDispatcher;
import com.lxbluem.irc.domain.exception.BotNotFoundException;
import com.lxbluem.irc.domain.model.BotState;
import com.lxbluem.irc.domain.model.request.DccFinishedExitCommand;
import com.lxbluem.irc.domain.model.request.ExitCommand;
import com.lxbluem.irc.domain.model.request.ManualExitCommand;
import com.lxbluem.irc.domain.ports.incoming.ExitBot;
import com.lxbluem.irc.domain.ports.outgoing.BotStateStorage;
import com.lxbluem.irc.domain.ports.outgoing.BotStorage;

import java.time.Clock;
import java.time.Instant;

public class ExitBotImpl implements ExitBot {
    private final BotStorage botStorage;
    private final BotStateStorage stateStorage;
    private final EventDispatcher eventDispatcher;
    private final Clock clock;

    public ExitBotImpl(
            BotStorage botStorage,
            BotStateStorage stateStorage,
            EventDispatcher eventDispatcher,
            Clock clock) {
        this.botStorage = botStorage;
        this.stateStorage = stateStorage;
        this.eventDispatcher = eventDispatcher;
        this.clock = clock;
    }

    @Override
    public void handle(ExitCommand exitCommand) {
        String botNickName = exitCommand.getBotNickName();
        botStorage.get(botNickName)
                .orElseThrow(() ->
                        new BotNotFoundException(botNickName)
                );
        commonExit(botNickName, "requested shutdown");
    }

    @Override
    public void handle(ManualExitCommand manualExitCommand) {
        String botNickName = manualExitCommand.getBotNickName();
        String reason = manualExitCommand.getReason();
        commonExit(botNickName, reason);
    }

    @Override
    public void handle(DccFinishedExitCommand finishedExitCommand) {
        String botNickName = finishedExitCommand.getBotNickName();
        String reason = finishedExitCommand.getReason();
        stateStorage.get(botNickName).ifPresent(BotState::dccTransferStopped);
        commonExit(botNickName, reason);
    }

    private void commonExit(String botNickName, String reason) {
        botStorage.get(botNickName).ifPresent(ircBot -> {
            stateStorage.get(botNickName).ifPresent(state -> {
                        if (state.isDccTransferRunning())
                            ircBot.cancelDcc(state.getPack().getNickName());
                    }
            );

            ircBot.terminate();
        });
        botStorage.remove(botNickName);
        stateStorage.remove(botNickName);

        String reasonMessage = String.format("Bot %s exiting because %s", botNickName, reason);
        eventDispatcher.dispatch(new BotExitedEvent(botNickName, nowEpochMillis(), reasonMessage));
    }

    private long nowEpochMillis() {
        return Instant.now(clock).toEpochMilli();
    }
}
