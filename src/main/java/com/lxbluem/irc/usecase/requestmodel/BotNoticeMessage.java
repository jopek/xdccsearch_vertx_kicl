package com.lxbluem.irc.usecase.requestmodel;

import lombok.Builder;
import lombok.Getter;

@Getter
public class BotNoticeMessage extends BotMessage {
    private final String message;
    private final String remoteNick;

    @Builder
    public BotNoticeMessage(String bot, long timestamp, String remoteNick, String noticeMessage) {
        super(timestamp, bot);
        this.remoteNick = remoteNick;
        this.message = noticeMessage;
    }
}
