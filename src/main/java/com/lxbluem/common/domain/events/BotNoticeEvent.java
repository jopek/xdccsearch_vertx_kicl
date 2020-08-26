package com.lxbluem.common.domain.events;

import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;

@Getter
@NoArgsConstructor
@ToString
public class BotNoticeEvent extends BotEvent {
    private String message;
    private String remoteNick;

    @Builder
    public BotNoticeEvent(String bot, long timestamp, String remoteNick, String noticeMessage) {
        super(timestamp, bot);
        this.remoteNick = remoteNick;
        this.message = noticeMessage;
    }
}
