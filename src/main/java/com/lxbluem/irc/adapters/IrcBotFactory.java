package com.lxbluem.irc.adapters;

import com.lxbluem.irc.domain.BotService;
import com.lxbluem.irc.domain.ports.BotFactory;
import com.lxbluem.irc.domain.ports.IrcBot;

public class IrcBotFactory implements BotFactory {

    @Override
    public IrcBot createNewInstance(BotService botService) {
        return new KittehIrcBot(botService);
    }
}
