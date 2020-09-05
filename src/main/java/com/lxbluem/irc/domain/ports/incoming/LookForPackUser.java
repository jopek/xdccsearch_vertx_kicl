package com.lxbluem.irc.domain.ports.incoming;

import com.lxbluem.irc.domain.model.request.LookForPackUserCommand;

public interface LookForPackUser {
    void handle(LookForPackUserCommand command);
}
