package com.lxbluem.irc.usecase.requestmodel;

import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;

@Getter
@NoArgsConstructor
@ToString
public class BotNoticeMessage extends BotMessage {
    private String message;
    private String remoteNick;

    @Builder
    public BotNoticeMessage(String bot, long timestamp, String remoteNick, String noticeMessage) {
        super(timestamp, bot);
        this.remoteNick = remoteNick;
        this.message = noticeMessage;
    }
}
