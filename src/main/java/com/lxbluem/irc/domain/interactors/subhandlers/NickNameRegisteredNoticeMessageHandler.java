package com.lxbluem.irc.domain.interactors.subhandlers;

import com.lxbluem.irc.domain.model.State;
import com.lxbluem.irc.domain.model.request.NoticeMessageCommand;
import com.lxbluem.irc.domain.ports.incoming.NoticeMessageHandler;
import com.lxbluem.irc.domain.ports.outgoing.StateStorage;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class NickNameRegisteredNoticeMessageHandler implements NoticeMessageHandler.SubHandler {
    private final StateStorage stateStorage;

    public NickNameRegisteredNoticeMessageHandler(StateStorage stateStorage) {
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
                .ifPresent(State::nickRegistered);
        return true;
    }
}
