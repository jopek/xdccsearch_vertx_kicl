package com.lxbluem.irc.domain.ports.incoming;

import com.lxbluem.irc.domain.model.request.InitializeBotCommand;

public interface InitializeBot {
    String handle(InitializeBotCommand initializeBotCommand);
}
