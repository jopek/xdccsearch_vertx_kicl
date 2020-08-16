package com.lxbluem.irc.adapter;

import com.lxbluem.irc.domain.DccBotState;
import com.lxbluem.irc.usecase.ports.DccBotStateStorage;

import java.util.HashMap;
import java.util.Map;

public class InMemoryBotStateStorage implements DccBotStateStorage {
    private final Map<String, DccBotState> botStates = new HashMap<>();

    @Override
    public DccBotState getBotStateByNick(String botNick) {
        return botStates.get(botNick);
    }

    @Override
    public DccBotState save(String botNick, DccBotState dccBotState) {
        return botStates.put(botNick, dccBotState);
    }
}
