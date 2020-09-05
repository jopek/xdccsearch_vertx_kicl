package com.lxbluem.irc.domain.interactors.subhandlers;

import com.lxbluem.irc.domain.model.request.NoticeMessageCommand;
import com.lxbluem.irc.domain.ports.incoming.NoticeMessageHandler;
import com.lxbluem.irc.domain.ports.outgoing.BotStateStorage;
import com.lxbluem.irc.domain.ports.outgoing.BotStorage;

public class RegisterNickNameNoticeMessageHandler implements NoticeMessageHandler.SubHandler {
    private final BotStorage botStorage;
    private final BotStateStorage stateStorage;

    public RegisterNickNameNoticeMessageHandler(BotStorage botStorage, BotStateStorage stateStorage) {
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
                stateStorage.get(botNickName).ifPresent(botState -> {
                            botState.nickRegistryRequired();
                            bot.registerNickname(botNickName);
                        }
                )
        );

        return true;
    }
}
