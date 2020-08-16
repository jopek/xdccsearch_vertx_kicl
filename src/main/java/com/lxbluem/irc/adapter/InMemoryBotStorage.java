package com.lxbluem.irc.adapter;

import com.lxbluem.irc.usecase.ports.BotPort;
import com.lxbluem.irc.usecase.ports.BotStorage;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class InMemoryBotStorage implements BotStorage {
    private final Map<String, BotPort> bots = new HashMap<>();

    @Override
    public BotPort getBotByNick(String botNick) {
        return bots.get(botNick);
    }

    @Override
    public void save(String botNick, BotPort bot) {
        bots.put(botNick, bot);
    }

    @Override
    public List<String> botNames() {
        return new ArrayList<>(bots.keySet());
    }
}
