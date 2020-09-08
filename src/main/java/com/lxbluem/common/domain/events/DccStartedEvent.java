package com.lxbluem.common.domain.events;

import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class DccStartedEvent extends BotEvent {
    @Builder
    public DccStartedEvent(String botName, long timestamp) {
        super(timestamp, botName);
    }
}

