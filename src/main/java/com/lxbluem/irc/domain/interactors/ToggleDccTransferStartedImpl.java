package com.lxbluem.irc.domain.interactors;

import com.lxbluem.irc.domain.model.BotState;
import com.lxbluem.irc.domain.model.request.ToggleDccTransferStartedCommand;
import com.lxbluem.irc.domain.ports.incoming.ToggleDccTransferStarted;
import com.lxbluem.irc.domain.ports.outgoing.BotStateStorage;

public class ToggleDccTransferStartedImpl implements ToggleDccTransferStarted {
    private final BotStateStorage stateStorage;

    public ToggleDccTransferStartedImpl(BotStateStorage stateStorage) {
        this.stateStorage = stateStorage;
    }

    @Override
    public void handle(ToggleDccTransferStartedCommand command) {
        stateStorage.get(command.getBotNickName()).ifPresent(BotState::dccTransferRunning);
    }
}
