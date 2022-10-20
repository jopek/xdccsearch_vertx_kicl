package com.lxbluem.irc.adapters;

import com.lxbluem.irc.domain.ports.incoming.ChangeNickName;
import com.lxbluem.irc.domain.ports.incoming.ExitBot;
import com.lxbluem.irc.domain.ports.incoming.JoinMentionedChannels;
import com.lxbluem.irc.domain.ports.incoming.LookForPackUser;
import com.lxbluem.irc.domain.ports.incoming.NoticeMessageHandler;
import com.lxbluem.irc.domain.ports.incoming.PrepareDccTransfer;
import com.lxbluem.irc.domain.ports.incoming.RegisterNickName;
import com.lxbluem.irc.domain.ports.incoming.SkipProtectedChannel;
import com.lxbluem.irc.domain.ports.outgoing.BotFactory;
import com.lxbluem.irc.domain.ports.outgoing.IrcBot;

public class KittehIrcBotFactory implements BotFactory {

    private final ExitBot exitBot;
    private final NoticeMessageHandler noticeMessageHandler;
    private final PrepareDccTransfer prepareDccTransfer;
    private final LookForPackUser lookForPackUser;
    private final JoinMentionedChannels joinMentionedChannels;
    private final RegisterNickName registerNickName;
    private final ChangeNickName changeNickName;
    private final SkipProtectedChannel skipProtectedChannel;

    public KittehIrcBotFactory(
            ExitBot exitBot,
            NoticeMessageHandler noticeMessageHandler,
            PrepareDccTransfer prepareDccTransfer,
            LookForPackUser lookForPackUser,
            JoinMentionedChannels joinMentionedChannels,
            RegisterNickName registerNickName,
            ChangeNickName changeNickName,
            SkipProtectedChannel skipProtectedChannel) {
        this.exitBot = exitBot;
        this.noticeMessageHandler = noticeMessageHandler;
        this.prepareDccTransfer = prepareDccTransfer;
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
                prepareDccTransfer,
                lookForPackUser,
                joinMentionedChannels,
                registerNickName,
                changeNickName,
                skipProtectedChannel
        );
    }
}
