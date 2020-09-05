package com.lxbluem.irc.domain.ports.incoming;

import com.lxbluem.irc.domain.model.request.SkipProtectedChannelCommand;

public interface SkipProtectedChannel {
    void handle(SkipProtectedChannelCommand command);
}
