package com.lxbluem.irc.usecase.ports;

import com.lxbluem.irc.domain.DccBotState;

public interface DccBotStateStorage {
    DccBotState getBotStateByNick(String botNick);
    DccBotState save(String botNick, DccBotState dccBotState);
}
