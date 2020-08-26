package com.lxbluem.irc.domain.ports;

import com.lxbluem.irc.domain.model.request.BotConnectionDetails;

public interface BotPort {
    void connect(BotConnectionDetails connectionDetails);

    void joinChannel(String... channelName);

    void registerNickname(String botName);

    void changeNickname(String newBotname);

    void requestDccPack(String remoteBotName, int packNumber);

    void sendCtcpMessage(String remoteBotName, String message);

    void terminate();
}
