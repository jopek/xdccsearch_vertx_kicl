package com.lxbluem.irc.usecase.requestmodel;

import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class BotFailMessage extends BotMessage {
    private String message;

    @Builder
    public BotFailMessage(String bot, long timestamp, String failMessage) {
        super(timestamp, bot);
        this.message = failMessage;
    }
}
