package com.lxbluem.irc.adapters;

import com.lxbluem.irc.domain.ports.outgoing.BotStorage;
import com.lxbluem.irc.domain.ports.outgoing.IrcBot;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public class InMemoryBotStorage implements BotStorage {
    private final Map<String, IrcBot> bots = new HashMap<>();

    @Override
    public Optional<IrcBot> get(String botNick) {
        return Optional.ofNullable(bots.get(botNick));
    }

    @Override
    public void save(String botNick, IrcBot bot) {
        bots.put(botNick, bot);
    }

    @Override
    public void remove(String botNickName) {
        bots.remove(botNickName);
    }
}
