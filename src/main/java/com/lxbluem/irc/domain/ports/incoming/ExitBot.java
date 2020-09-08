package com.lxbluem.irc.domain.ports.incoming;

import com.lxbluem.irc.domain.model.request.DccFinishedExitCommand;
import com.lxbluem.irc.domain.model.request.ExitCommand;
import com.lxbluem.irc.domain.model.request.ManualExitCommand;

public interface ExitBot {
    void handle(ExitCommand exitCommand);

    void handle(ManualExitCommand manualExitCommand);

    void handle(DccFinishedExitCommand finishedExitCommand);
}
