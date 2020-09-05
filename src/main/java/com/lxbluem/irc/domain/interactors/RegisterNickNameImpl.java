package com.lxbluem.irc.domain.interactors;

import com.lxbluem.irc.domain.model.request.RegisterNickNameCommand;
import com.lxbluem.irc.domain.ports.incoming.RegisterNickName;
import com.lxbluem.irc.domain.ports.outgoing.BotStorage;

public class RegisterNickNameImpl implements RegisterNickName {
    private final BotStorage botStorage;

    public RegisterNickNameImpl(BotStorage botStorage) {
        this.botStorage = botStorage;
    }

    @Override
    public void handle(RegisterNickNameCommand command) {
        String botNickName = command.getBotNickName();
        botStorage.get(botNickName).ifPresent(bot ->
                bot.registerNickname(botNickName)
        );

    }

}
