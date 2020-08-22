package com.lxbluem.irc.usecase.requestmodel;

import lombok.Builder;
import lombok.Getter;

@Getter
public class BotExitMessage extends BotMessage {
    private final String message;

    @Builder
    public BotExitMessage(String bot, long timestamp, String message) {
        super(timestamp, bot);
        this.message = message;
    }
}