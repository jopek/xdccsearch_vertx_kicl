package com.lxbluem.irc.domain.ports;

import com.lxbluem.irc.domain.BotService;

public interface BotFactory {
    IrcBot createNewInstance(BotService botService);
}
