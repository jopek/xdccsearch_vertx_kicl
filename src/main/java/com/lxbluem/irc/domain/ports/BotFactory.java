package com.lxbluem.irc.domain.ports;

import com.lxbluem.irc.domain.BotService;

public interface BotFactory {
    BotPort createNewInstance(BotService botService);
}
