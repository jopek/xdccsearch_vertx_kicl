package com.lxbluem.irc.usecase.ports;

import com.lxbluem.irc.usecase.requestmodel.BotConnectionDetails;

public interface BotPort {
    void connect(BotConnectionDetails connectionDetails);
    void joinChannel(String... channelName);
    void registerNickname(String botName);
    void changeNickname(String newBotname);
    void requestDccPack(String remoteBotName, int packNumber);
    void sendCtcpMessage(String remoteBotName, String message);
    void terminate();
}
