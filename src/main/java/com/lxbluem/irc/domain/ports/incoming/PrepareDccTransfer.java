package com.lxbluem.irc.domain.ports.incoming;

import com.lxbluem.irc.domain.model.request.PrepareDccTransferCommand;

public interface PrepareDccTransfer {
    void handle(PrepareDccTransferCommand command);
}
