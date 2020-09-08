package com.lxbluem.irc.adapters;

import com.lxbluem.irc.domain.model.State;
import com.lxbluem.irc.domain.ports.outgoing.StateStorage;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public class InMemoryStateStorage implements StateStorage {
    private final Map<String, State> stateMap = new HashMap<>();

    @Override
    public Optional<State> get(String botNickName) {
        return Optional.ofNullable(stateMap.get(botNickName));
    }

    @Override
    public State save(String botNickName, State state) {
        return stateMap.put(botNickName, state);
    }

    @Override
    public State remove(String botNickName) {
        return stateMap.remove(botNickName);
    }
}
