package com.lxbluem.common.domain.events;

import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class DccStartedEvent extends BotEvent {
    @Builder
    public DccStartedEvent(String botNickName) {
        super.bot = botNickName;
    }
}

