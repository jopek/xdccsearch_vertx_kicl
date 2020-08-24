package com.lxbluem.irc.usecase.requestmodel;

import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class BotExitMessage extends BotMessage {
    private String message;

    @Builder
    public BotExitMessage(String bot, long timestamp, String message) {
        super(timestamp, bot);
        this.message = message;
    }
}