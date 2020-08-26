package com.lxbluem.common.domain.events;

import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class BotEvent extends Event {
    String bot;

    public BotEvent(long timestamp, String bot) {
        super(timestamp);
        this.bot = bot;
    }
}
