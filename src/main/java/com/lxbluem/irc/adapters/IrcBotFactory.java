package com.lxbluem.irc.adapters;

import com.lxbluem.irc.domain.BotService;
import com.lxbluem.irc.domain.ports.BotFactory;
import com.lxbluem.irc.domain.ports.BotPort;

public class IrcBotFactory implements BotFactory {

    @Override
    public BotPort createNewInstance(BotService botService) {
        return new KitehIrcBot(botService);
    }
}
