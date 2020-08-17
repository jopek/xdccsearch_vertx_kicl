package com.lxbluem.irc.usecase.requestmodel;

import lombok.Builder;
import lombok.Getter;

@Getter
public class BotFailMessage extends BotMessage {
    private final String message;

    @Builder
    public BotFailMessage(String bot, long timestamp, String failMessage) {
        super(timestamp, bot);
        this.message = failMessage;
    }
}
