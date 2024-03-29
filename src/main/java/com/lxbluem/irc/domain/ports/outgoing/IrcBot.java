package com.lxbluem.irc.domain.ports.outgoing;

import com.lxbluem.irc.domain.model.request.BotConnectionDetails;

import java.util.Collection;

public interface IrcBot {
    void connect(BotConnectionDetails connectionDetails);

    void joinChannel(Collection<String> channelNames);

    void joinChannel(String... channelNames);

    void registerNickname(String botName);

    void changeNickname(String newBotname);

    void requestDccPack(String remoteBotName, int packNumber);

    void sendCtcpMessage(String remoteBotName, String message);

    void terminate();

    void cancelDcc(String remoteBotName);

    void stopSearchListing(String remoteBotName);

    void startSearchListing(String remoteBotName, String packname);

    String getBotName();
}
