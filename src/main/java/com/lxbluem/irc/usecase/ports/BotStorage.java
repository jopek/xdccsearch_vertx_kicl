package com.lxbluem.irc.usecase.ports;

import java.util.List;
import java.util.Optional;

public interface BotStorage {
    Optional<BotPort> getBotByNick(String botNick);
    void save(String botNick, BotPort bot);
    List<String> botNames();
}
