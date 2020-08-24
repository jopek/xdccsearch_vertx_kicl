package com.lxbluem.irc.usecase.ports;

import com.lxbluem.irc.domain.DccBotState;

import java.util.Optional;

public interface DccBotStateStorage {
    Optional<DccBotState> getBotStateByNick(String botNickName);
    DccBotState save(String botNickName, DccBotState dccBotState);

    DccBotState removeBotState(String botNickName);
}
