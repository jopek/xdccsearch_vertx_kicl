package com.lxbluem.irc.adapter;

import com.lxbluem.irc.domain.DccBotState;
import com.lxbluem.irc.usecase.ports.DccBotStateStorage;

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
    public DccBotState save(String botNick, DccBotState dccBotState) {
        return botStates.put(botNick, dccBotState);
    }
}
