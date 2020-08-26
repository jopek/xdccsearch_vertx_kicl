package com.lxbluem.common.domain.events;

import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

//FIXME: renameto --> newBotName
@Getter
@NoArgsConstructor
public class BotRenamedEvent extends BotEvent {
    private String renameto;
    private String message;

    @Builder
    public BotRenamedEvent(long timestamp, String attemptedBotName, String renameto, String serverMessages) {
        super(timestamp, attemptedBotName);
        this.renameto = renameto;
        this.message = serverMessages;
    }
}
