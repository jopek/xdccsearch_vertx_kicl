package com.lxbluem.irc.domain.ports;

import com.lxbluem.irc.domain.model.BotState;

import java.util.Optional;

public interface BotStateStorage {
    Optional<BotState> get(String botNickName);

    BotState save(String botNickName, BotState botState);

    BotState remove(String botNickName);
}
