package com.lxbluem.irc.domain.interactors;

import com.lxbluem.irc.domain.model.State;
import com.lxbluem.irc.domain.model.request.ToggleDccTransferStartedCommand;
import com.lxbluem.irc.domain.ports.incoming.ToggleDccTransferStarted;
import com.lxbluem.irc.domain.ports.outgoing.StateStorage;

public class ToggleDccTransferStartedImpl implements ToggleDccTransferStarted {
    private final StateStorage stateStorage;

    public ToggleDccTransferStartedImpl(StateStorage stateStorage) {
        this.stateStorage = stateStorage;
    }

    @Override
    public void handle(ToggleDccTransferStartedCommand command) {
        stateStorage.get(command.getBotNickName()).ifPresent(State::dccTransferRunning);
    }
}
