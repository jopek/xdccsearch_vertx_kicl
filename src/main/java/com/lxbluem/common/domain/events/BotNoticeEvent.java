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
    public BotNoticeEvent(String botNickName, String remoteNick, String noticeMessage) {
        super.bot = botNickName;
        this.remoteNick = remoteNick;
        this.message = noticeMessage;
    }
}
