package com.lxbluem.irc.usecase.requestmodel;

import lombok.Builder;
import lombok.Getter;

@Getter
public class BotNoticeMessage extends BotMessage {
    private final String message;

    @Builder
    public BotNoticeMessage(String bot, long timestamp, String noticeMessage) {
        super(timestamp, bot);
        this.message = noticeMessage;
    }
}
