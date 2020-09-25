package com.lxbluem.irc.domain.ports.incoming;

import com.lxbluem.irc.domain.model.request.DccResumeAcceptTransferCommand;
import com.lxbluem.irc.domain.model.request.DccSendTransferCommand;

public interface PrepareDccTransfer {
    void handle(DccSendTransferCommand command);
    void handle(DccResumeAcceptTransferCommand command);
}
