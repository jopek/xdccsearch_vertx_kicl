package com.lxbluem.common.domain.events;

import com.lxbluem.common.domain.Pack;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class BotInitializedEvent extends BotEvent {
    private Pack pack;

    @Builder
    public BotInitializedEvent(String bot, long timestamp, Pack pack) {
        super(timestamp, bot);
        this.pack = pack;
    }
}
