package com.lxbluem.irc.domain.ports;

import java.util.Optional;

public interface BotStorage {
    Optional<IrcBot> get(String botNickName);

    void save(String botNickName, IrcBot bot);

    void remove(String botNickName);
}
