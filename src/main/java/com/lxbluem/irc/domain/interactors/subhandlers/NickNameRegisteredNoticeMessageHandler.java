package com.lxbluem.irc.domain.interactors.subhandlers;

import com.lxbluem.irc.domain.model.BotState;
import com.lxbluem.irc.domain.model.request.NoticeMessageCommand;
import com.lxbluem.irc.domain.ports.incoming.NoticeMessageHandler;
import com.lxbluem.irc.domain.ports.outgoing.BotStateStorage;
import com.lxbluem.irc.domain.ports.outgoing.BotStorage;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class NickNameRegisteredNoticeMessageHandler implements NoticeMessageHandler.SubHandler {
    private final BotStorage botStorage;
    private final BotStateStorage stateStorage;

    public NickNameRegisteredNoticeMessageHandler(BotStorage botStorage, BotStateStorage stateStorage) {
        this.botStorage = botStorage;
        this.stateStorage = stateStorage;
    }

    @Override
    public boolean handle(NoticeMessageCommand command) {
        String botNickName = command.getBotNickName();
        String remoteName = command.getRemoteName();
        String noticeMessage = command.getNoticeMessage();

        if (!remoteName.equalsIgnoreCase("nickserv"))
            return false;

        Pattern pattern = Pattern.compile("nickname \\w+ registered", Pattern.CASE_INSENSITIVE);
        Matcher matcher = pattern.matcher(noticeMessage);

        if (!matcher.find())
            return false;

        stateStorage.get(botNickName)
                .ifPresent(BotState::nickRegistered);
        return true;
    }
}
