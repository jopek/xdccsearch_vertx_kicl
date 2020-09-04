package com.lxbluem.irc.adapters;

import com.lxbluem.irc.domain.BotService;
import com.lxbluem.irc.domain.ports.incoming.ExitBot;
import com.lxbluem.irc.domain.ports.outgoing.BotFactory;
import com.lxbluem.irc.domain.ports.outgoing.IrcBot;

public class IrcBotFactory implements BotFactory {

    private final ExitBot exitBot;
    private final BotService botService;

    public IrcBotFactory(ExitBot exitBot, BotService botService) {
        this.exitBot = exitBot;
        this.botService = botService;
    }

    @Override
    public IrcBot createNewInstance() {
        return new KittehIrcBot(botService, exitBot);
    }
}
