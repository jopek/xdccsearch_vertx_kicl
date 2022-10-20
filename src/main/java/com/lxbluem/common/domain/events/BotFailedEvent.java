package com.lxbluem.common.domain.events;

import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@EqualsAndHashCode
public class BotFailedEvent extends BotEvent {
    private String message;

    @Builder
    public BotFailedEvent(String botNickName, String failMessage) {
        super.bot = botNickName;
        this.message = failMessage;
    }
}
