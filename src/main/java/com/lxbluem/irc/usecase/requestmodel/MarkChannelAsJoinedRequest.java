package com.lxbluem.irc.usecase.requestmodel;

import lombok.Builder;
import lombok.RequiredArgsConstructor;

@Builder
@RequiredArgsConstructor
public class MarkChannelAsJoinedRequest {
    final String botName;
    final String channelName;
}
