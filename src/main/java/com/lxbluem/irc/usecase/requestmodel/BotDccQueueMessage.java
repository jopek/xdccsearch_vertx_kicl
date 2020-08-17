package com.lxbluem.irc.usecase.requestmodel;

import lombok.Builder;
import lombok.Getter;

@Getter
public class BotDccQueueMessage extends BotMessage {
    private final String message;

    @Builder
    public BotDccQueueMessage(String botName, long timestamp, String noticeMessage) {
        super(timestamp, botName);
        message = noticeMessage;
    }
}
