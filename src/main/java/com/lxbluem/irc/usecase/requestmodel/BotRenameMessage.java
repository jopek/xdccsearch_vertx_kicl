package com.lxbluem.irc.usecase.requestmodel;

import lombok.Builder;
import lombok.Getter;

@Getter
public class BotRenameMessage extends BotMessage {
    private final String newBotName;
    private final String message;

    @Builder
    public BotRenameMessage(long timestamp, String attemptedBotName, String newBotName, String serverMessages) {
        super(timestamp, attemptedBotName);
        this.newBotName = newBotName;
        this.message = serverMessages;
    }
}
