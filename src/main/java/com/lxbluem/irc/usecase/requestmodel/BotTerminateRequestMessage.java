package com.lxbluem.irc.usecase.requestmodel;

import lombok.Getter;

import java.io.Serializable;

@Getter
public class BotTerminateRequestMessage extends BotMessage implements Serializable {

    public BotTerminateRequestMessage(String bot, long timestamp) {
        super(timestamp, bot);
    }
}
