package com.lxbluem.irc.adapters;

import com.lxbluem.irc.domain.BotService;
import com.lxbluem.irc.domain.ports.incoming.CtcpQueryHandler;
import com.lxbluem.irc.domain.ports.incoming.ExitBot;
import com.lxbluem.irc.domain.ports.incoming.NoticeMessageHandler;
import com.lxbluem.irc.domain.ports.incoming.UsersInChannel;
import com.lxbluem.irc.domain.ports.outgoing.BotFactory;
import com.lxbluem.irc.domain.ports.outgoing.IrcBot;

public class KittehIrcBotFactory implements BotFactory {

    private final ExitBot exitBot;
    private final BotService botService;
    private final NoticeMessageHandler noticeMessageHandler;
    private final CtcpQueryHandler ctcpQueryHandler;
    private final UsersInChannel usersInChannel;

    public KittehIrcBotFactory(
            ExitBot exitBot,
            NoticeMessageHandler noticeMessageHandler,
            BotService botService,
            CtcpQueryHandler ctcpQueryHandler,
            UsersInChannel usersInChannel) {
        this.exitBot = exitBot;
        this.noticeMessageHandler = noticeMessageHandler;
        this.botService = botService;
        this.ctcpQueryHandler = ctcpQueryHandler;
        this.usersInChannel = usersInChannel;
    }

    @Override
    public IrcBot createNewInstance() {
        return new KittehIrcBot(botService, exitBot, noticeMessageHandler, ctcpQueryHandler, usersInChannel);
    }
}
