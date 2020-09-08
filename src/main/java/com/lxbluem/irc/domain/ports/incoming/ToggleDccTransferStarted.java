package com.lxbluem.irc.domain.ports.incoming;

import com.lxbluem.irc.domain.model.request.ToggleDccTransferStartedCommand;

public interface ToggleDccTransferStarted {
    void handle(ToggleDccTransferStartedCommand command);
}
