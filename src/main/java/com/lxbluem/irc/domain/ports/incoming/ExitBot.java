package com.lxbluem.irc.domain.ports.incoming;

import com.lxbluem.irc.domain.model.request.DccFinishedExitCommand;
import com.lxbluem.irc.domain.model.request.ReasonedExitCommand;
import com.lxbluem.irc.domain.model.request.RequestedExitCommand;

public interface ExitBot {
    void handle(RequestedExitCommand requestedExitCommand);

    void handle(ReasonedExitCommand reasonedExitCommand);

    void handle(DccFinishedExitCommand finishedExitCommand);
}
