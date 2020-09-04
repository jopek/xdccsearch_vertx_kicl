package com.lxbluem.irc.adapters;

import com.lxbluem.irc.domain.BotService;
import com.lxbluem.irc.domain.ports.incoming.ExitBot;
import com.lxbluem.irc.domain.ports.incoming.NoticeMessageHandler;
import com.lxbluem.irc.domain.ports.outgoing.BotFactory;
import com.lxbluem.irc.domain.ports.outgoing.IrcBot;

public class KittehIrcBotFactory implements BotFactory {

    private final ExitBot exitBot;
    private final BotService botService;
    private final NoticeMessageHandler noticeMessageHandler;

    public KittehIrcBotFactory(ExitBot exitBot, NoticeMessageHandler noticeMessageHandler, BotService botService) {
        this.exitBot = exitBot;
        this.noticeMessageHandler = noticeMessageHandler;
        this.botService = botService;
    }

    @Override
    public IrcBot createNewInstance() {
        return new KittehIrcBot(botService, exitBot, noticeMessageHandler);
    }
}
