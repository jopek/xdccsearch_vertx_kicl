package com.lxbluem.common.domain.events;

import lombok.*;

@Getter
@NoArgsConstructor
@ToString
@EqualsAndHashCode(callSuper = true)
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
