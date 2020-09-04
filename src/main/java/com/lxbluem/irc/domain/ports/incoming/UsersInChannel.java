package com.lxbluem.irc.domain.ports.incoming;

import com.lxbluem.irc.domain.model.request.UsersInChannelCommand;

public interface UsersInChannel {
    void handle(UsersInChannelCommand command);
}
