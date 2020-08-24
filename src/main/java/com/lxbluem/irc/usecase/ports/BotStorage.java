package com.lxbluem.irc.usecase.ports;

import java.util.List;
import java.util.Optional;

public interface BotStorage {
    Optional<BotPort> getBotByNick(String botNickName);
    void save(String botNickName, BotPort bot);
    List<String> botNickNames();
    void removeBot(String botNickName);
}
