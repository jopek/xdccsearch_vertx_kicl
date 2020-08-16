package com.lxbluem.irc.adapter;

import com.lxbluem.irc.usecase.BotFactory;
import com.lxbluem.irc.usecase.BotService;
import com.lxbluem.irc.usecase.ports.BotPort;

public class IrcBotFactory implements BotFactory {

    @Override
    public BotPort createNewInstance(BotService botService) {
        return new KitehIrcBot(botService);
    }
}
