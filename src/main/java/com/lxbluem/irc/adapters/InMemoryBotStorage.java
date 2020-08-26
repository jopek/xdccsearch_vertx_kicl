package com.lxbluem.irc.adapters;

import com.lxbluem.irc.domain.ports.BotPort;
import com.lxbluem.irc.domain.ports.BotStorage;

import java.util.*;

public class InMemoryBotStorage implements BotStorage {
    private final Map<String, BotPort> bots = new HashMap<>();

    @Override
    public Optional<BotPort> getBotByNick(String botNick) {
        return Optional.ofNullable(bots.get(botNick));
    }

    @Override
    public void save(String botNick, BotPort bot) {
        bots.put(botNick, bot);
    }

    @Override
    public List<String> botNickNames() {
        return new ArrayList<>(bots.keySet());
    }

    @Override
    public void removeBot(String botNickName) {
        bots.remove(botNickName);
    }
}