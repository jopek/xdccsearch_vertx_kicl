package com.lxbluem.state.domain.ports;

import com.lxbluem.state.domain.model.State;

import java.util.Map;

public interface StateRepository {
    State getStateByBotName(String botName);

    State saveStateByBotName(String botName, State state);

    Map<String, State> getStateEntries();

    State removeStateByBotName(String botname);
}
