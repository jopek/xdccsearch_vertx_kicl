package com.lxbluem.irc.domain.model.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@AllArgsConstructor
@Builder
@Getter
public class JoinMentionedChannelsCommand {
    private final String botNickName;
    private final String channelName;
    private final String topic;
}
