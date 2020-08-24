package com.lxbluem.irc.usecase.requestmodel;

import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class BotDccQueueMessage extends BotMessage {
    private String message;

    @Builder
    public BotDccQueueMessage(String botName, long timestamp, String noticeMessage) {
        super(timestamp, botName);
        message = noticeMessage;
    }
}
