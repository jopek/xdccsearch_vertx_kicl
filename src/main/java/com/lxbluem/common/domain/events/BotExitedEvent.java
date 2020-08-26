package com.lxbluem.common.domain.events;

import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class BotExitedEvent extends BotEvent {
    private String message;

    @Builder
    public BotExitedEvent(String bot, long timestamp, String message) {
        super(timestamp, bot);
        this.message = message;
    }
}