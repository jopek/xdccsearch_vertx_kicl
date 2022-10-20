package com.lxbluem.common.domain.events;

import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@EqualsAndHashCode
public class BotExitedEvent extends BotEvent {
    private String message;

    @Builder
    public BotExitedEvent(String botNickName, String message) {
        super.bot = botNickName;
        this.message = message;
    }
}
