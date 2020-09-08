package com.lxbluem.irc.domain.interactors.subhandlers;

import com.lxbluem.irc.domain.model.request.NoticeMessageCommand;
import com.lxbluem.irc.domain.ports.incoming.NoticeMessageHandler;
import com.lxbluem.irc.domain.ports.outgoing.BotStorage;
import com.lxbluem.irc.domain.ports.outgoing.StateStorage;

public class RegisterNickNameNoticeMessageHandler implements NoticeMessageHandler.SubHandler {
    private final BotStorage botStorage;
    private final StateStorage stateStorage;

    public RegisterNickNameNoticeMessageHandler(BotStorage botStorage, StateStorage stateStorage) {
        this.botStorage = botStorage;
        this.stateStorage = stateStorage;
    }

    @Override
    public boolean handle(NoticeMessageCommand command) {
        String botNickName = command.getBotNickName();
        String remoteName = command.getRemoteName();
        String noticeMessage = command.getNoticeMessage();
        String lowerCaseNoticeMessage = noticeMessage.toLowerCase();

        if (!(remoteName.equalsIgnoreCase("nickserv")))
            return false;
        if (!lowerCaseNoticeMessage.contains("your nickname is not registered. to register it, use"))
            return false;

        botStorage.get(botNickName).ifPresent(bot ->
                stateStorage.get(botNickName).ifPresent(state -> {
                            state.nickRegistryRequired();
                            bot.registerNickname(botNickName);
                        }
                )
        );

        return true;
    }
}
