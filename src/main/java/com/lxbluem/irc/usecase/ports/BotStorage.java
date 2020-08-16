package com.lxbluem.irc.usecase.ports;

import java.util.List;

public interface BotStorage {
    BotPort getBotByNick(String botNick);
    void save(String botNick, BotPort bot);
    List<String> botNames();
}
