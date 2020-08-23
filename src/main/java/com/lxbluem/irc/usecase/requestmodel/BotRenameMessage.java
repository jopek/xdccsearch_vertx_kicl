package com.lxbluem.irc.usecase.requestmodel;

import lombok.Builder;
import lombok.Getter;

//FIXME: renameto --> newBotName
@Getter
public class BotRenameMessage extends BotMessage {
    private final String renameto;
    private final String message;

    @Builder
    public BotRenameMessage(long timestamp, String attemptedBotName, String renameto, String serverMessages) {
        super(timestamp, attemptedBotName);
        this.renameto = renameto;
        this.message = serverMessages;
    }
}
