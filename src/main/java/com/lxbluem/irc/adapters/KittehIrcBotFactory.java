package com.lxbluem.irc.adapters;

import com.lxbluem.irc.domain.ports.incoming.*;
import com.lxbluem.irc.domain.ports.outgoing.BotFactory;
import com.lxbluem.irc.domain.ports.outgoing.IrcBot;

public class KittehIrcBotFactory implements BotFactory {

    private final ExitBot exitBot;
    private final NoticeMessageHandler noticeMessageHandler;
    private final StartDccTransfer startDccTransfer;
    private final LookForPackUser lookForPackUser;
    private final JoinMentionedChannels joinMentionedChannels;
    private final RegisterNickName registerNickName;
    private final ChangeNickName changeNickName;
    private final SkipProtectedChannel skipProtectedChannel;

    public KittehIrcBotFactory(
            ExitBot exitBot,
            NoticeMessageHandler noticeMessageHandler,
            StartDccTransfer startDccTransfer,
            LookForPackUser lookForPackUser,
            JoinMentionedChannels joinMentionedChannels,
            RegisterNickName registerNickName,
            ChangeNickName changeNickName,
            SkipProtectedChannel skipProtectedChannel) {
        this.exitBot = exitBot;
        this.noticeMessageHandler = noticeMessageHandler;
        this.startDccTransfer = startDccTransfer;
        this.lookForPackUser = lookForPackUser;
        this.joinMentionedChannels = joinMentionedChannels;
        this.registerNickName = registerNickName;
        this.changeNickName = changeNickName;
        this.skipProtectedChannel = skipProtectedChannel;
    }

    @Override
    public IrcBot createNewInstance() {
        return new KittehIrcBot(
                exitBot,
                noticeMessageHandler,
                startDccTransfer,
                lookForPackUser,
                joinMentionedChannels,
                registerNickName,
                changeNickName,
                skipProtectedChannel
        );
    }
}
