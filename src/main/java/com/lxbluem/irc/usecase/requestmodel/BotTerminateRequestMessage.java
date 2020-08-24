package com.lxbluem.irc.usecase.requestmodel;

import lombok.Getter;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Getter
@NoArgsConstructor
public class BotTerminateRequestMessage extends BotMessage implements Serializable {

    public BotTerminateRequestMessage(String bot, long timestamp) {
        super(timestamp, bot);
    }
}
