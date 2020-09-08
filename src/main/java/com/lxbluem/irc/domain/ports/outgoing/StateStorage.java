package com.lxbluem.irc.domain.ports.outgoing;

import com.lxbluem.irc.domain.model.State;

import java.util.Optional;

public interface StateStorage {
    Optional<State> get(String botNickName);

    State save(String botNickName, State state);

    State remove(String botNickName);
}
