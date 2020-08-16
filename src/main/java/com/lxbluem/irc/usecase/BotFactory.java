package com.lxbluem.irc.usecase;

import com.lxbluem.irc.usecase.ports.BotPort;

public interface BotFactory {
    BotPort createNewInstance(BotService botService);
}
