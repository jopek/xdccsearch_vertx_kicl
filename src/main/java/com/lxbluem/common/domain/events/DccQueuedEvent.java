package com.lxbluem.common.domain.events;

import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class DccQueuedEvent extends BotEvent {
    private String message;

    @Builder
    public DccQueuedEvent(String botName, long timestamp, String noticeMessage) {
        super(timestamp, botName);
        message = noticeMessage;
    }
}
