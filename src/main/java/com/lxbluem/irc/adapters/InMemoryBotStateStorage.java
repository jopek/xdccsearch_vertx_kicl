package com.lxbluem.irc.adapters;

import com.lxbluem.irc.domain.model.DccBotState;
import com.lxbluem.irc.domain.ports.DccBotStateStorage;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public class InMemoryBotStateStorage implements DccBotStateStorage {
    private final Map<String, DccBotState> botStates = new HashMap<>();

    @Override
    public Optional<DccBotState> getBotStateByNick(String botNickName) {
        return Optional.ofNullable(botStates.get(botNickName));
    }

    @Override
    public DccBotState save(String botNickName, DccBotState dccBotState) {
        return botStates.put(botNickName, dccBotState);
    }

    @Override
    public DccBotState removeBotState(String botNickName) {
        return botStates.remove(botNickName);
    }
}
