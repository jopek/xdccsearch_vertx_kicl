package com.lxbluem.irc.adapters;

import com.lxbluem.irc.domain.model.BotState;
import com.lxbluem.irc.domain.ports.BotStateStorage;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public class InMemoryBotStateStorage implements BotStateStorage {
    private final Map<String, BotState> botStates = new HashMap<>();

    @Override
    public Optional<BotState> getBotStateByNick(String botNickName) {
        return Optional.ofNullable(botStates.get(botNickName));
    }

    @Override
    public BotState save(String botNickName, BotState botState) {
        return botStates.put(botNickName, botState);
    }

    @Override
    public BotState removeBotState(String botNickName) {
        return botStates.remove(botNickName);
    }
}
