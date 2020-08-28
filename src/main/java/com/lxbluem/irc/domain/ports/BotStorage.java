package com.lxbluem.irc.domain.ports;

import java.util.List;
import java.util.Optional;

public interface BotStorage {
    Optional<IrcBot> getBotByNick(String botNickName);

    void save(String botNickName, IrcBot bot);

    List<String> botNickNames();

    void removeBot(String botNickName);
}
