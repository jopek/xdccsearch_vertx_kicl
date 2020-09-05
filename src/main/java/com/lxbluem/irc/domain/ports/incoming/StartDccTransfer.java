package com.lxbluem.irc.domain.ports.incoming;

import com.lxbluem.irc.domain.model.request.StartDccTransferCommand;

public interface StartDccTransfer {
    void handle(StartDccTransferCommand command);
}
