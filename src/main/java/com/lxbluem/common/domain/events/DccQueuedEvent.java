package com.lxbluem.common.domain.events;

import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class DccQueuedEvent extends BotEvent {
    private String message;

    @Builder
    public DccQueuedEvent(String botNickName, String noticeMessage) {
        super.bot = botNickName;
        message = noticeMessage;
    }
}
