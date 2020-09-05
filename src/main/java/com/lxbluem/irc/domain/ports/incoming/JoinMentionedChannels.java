package com.lxbluem.irc.domain.ports.incoming;

import com.lxbluem.irc.domain.model.request.JoinMentionedChannelsCommand;

public interface JoinMentionedChannels {
    void handle(JoinMentionedChannelsCommand command);
}
