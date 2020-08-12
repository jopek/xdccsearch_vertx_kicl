package com.lxbluem.state.adapters;

import com.lxbluem.state.domain.model.State;
import com.lxbluem.state.domain.ports.StateRepository;

import java.util.HashMap;
import java.util.Map;

public class InMemoryStateRepository implements StateRepository {

    private final Map<String, State> stateMap = new HashMap<>();

    @Override
    public State getStateByBotName(String botName) {
        return stateMap.get(botName);
    }

    @Override
    public State saveStateByBotName(String botName, State state) {
        return stateMap.put(botName, state);
    }

    @Override
    public Map<String, State> getStateEntries() {
        return stateMap;
    }

    @Override
    public State removeStateByBotName(String botname) {
        return stateMap.remove(botname);
    }
}
