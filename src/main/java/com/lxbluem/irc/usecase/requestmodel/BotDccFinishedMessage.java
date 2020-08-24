package com.lxbluem.irc.usecase.requestmodel;

import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class BotDccFinishedMessage extends BotMessage {
    @Builder
    public BotDccFinishedMessage(String botName, long timestamp) {
        super(timestamp, botName);
    }
}

