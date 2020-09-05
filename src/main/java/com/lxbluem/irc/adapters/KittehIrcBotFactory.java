package com.lxbluem.irc.adapters;

import com.lxbluem.irc.domain.BotService;
import com.lxbluem.irc.domain.ports.incoming.*;
import com.lxbluem.irc.domain.ports.outgoing.BotFactory;
import com.lxbluem.irc.domain.ports.outgoing.IrcBot;

public class KittehIrcBotFactory implements BotFactory {

    private final ExitBot exitBot;
    private final BotService botService;
    private final NoticeMessageHandler noticeMessageHandler;
    private final CtcpQueryHandler ctcpQueryHandler;
    private final LookForPackUser lookForPackUser;
    private final JoinMentionedChannels joinMentionedChannels;

    public KittehIrcBotFactory(
            ExitBot exitBot,
            NoticeMessageHandler noticeMessageHandler,
            BotService botService,
            CtcpQueryHandler ctcpQueryHandler,
            LookForPackUser lookForPackUser,
            JoinMentionedChannels joinMentionedChannels) {
        this.exitBot = exitBot;
        this.noticeMessageHandler = noticeMessageHandler;
        this.botService = botService;
        this.ctcpQueryHandler = ctcpQueryHandler;
        this.lookForPackUser = lookForPackUser;
        this.joinMentionedChannels = joinMentionedChannels;
    }

    @Override
    public IrcBot createNewInstance() {
        return new KittehIrcBot(botService,
                exitBot,
                noticeMessageHandler,
                ctcpQueryHandler,
                lookForPackUser,
                joinMentionedChannels);
    }
}
