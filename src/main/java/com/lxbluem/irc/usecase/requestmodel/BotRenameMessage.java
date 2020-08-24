package com.lxbluem.irc.usecase.requestmodel;

import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

//FIXME: renameto --> newBotName
@Getter
@NoArgsConstructor
public class BotRenameMessage extends BotMessage {
    private String renameto;
    private String message;

    @Builder
    public BotRenameMessage(long timestamp, String attemptedBotName, String renameto, String serverMessages) {
        super(timestamp, attemptedBotName);
        this.renameto = renameto;
        this.message = serverMessages;
    }
}
