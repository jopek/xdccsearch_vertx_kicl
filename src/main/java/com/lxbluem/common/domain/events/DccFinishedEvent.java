package com.lxbluem.common.domain.events;

import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class DccFinishedEvent extends BotEvent {
    @Builder
    public DccFinishedEvent(String botName, long timestamp) {
        super(timestamp, botName);
    }
}

