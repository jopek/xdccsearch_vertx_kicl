package com.lxbluem.irc.adapters;

import com.lxbluem.irc.domain.ports.BotStorage;
import com.lxbluem.irc.domain.ports.IrcBot;

import java.util.*;

public class InMemoryBotStorage implements BotStorage {
    private final Map<String, IrcBot> bots = new HashMap<>();

    @Override
    public Optional<IrcBot> getBotByNick(String botNick) {
        return Optional.ofNullable(bots.get(botNick));
    }

    @Override
    public void save(String botNick, IrcBot bot) {
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
