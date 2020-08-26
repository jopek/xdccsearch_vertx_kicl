package com.lxbluem.irc.domain.ports;

import com.lxbluem.irc.domain.model.DccBotState;

import java.util.Optional;

public interface DccBotStateStorage {
    Optional<DccBotState> getBotStateByNick(String botNickName);

    DccBotState save(String botNickName, DccBotState dccBotState);

    DccBotState removeBotState(String botNickName);
}
