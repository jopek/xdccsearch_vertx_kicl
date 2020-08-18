package com.lxbluem.irc.usecase.ports;

import com.lxbluem.irc.usecase.requestmodel.BotConnectionDetails;

public interface BotPort {
    void connect(BotConnectionDetails connectionDetails);
    void joinChannel(String... channelName);
    void registerNickname(String nick);
    void changeNickname(String newNick);
    void requestDccPack(String nick, int packNumber);
    void sendCtcpMessage(String nick, String message);
}
