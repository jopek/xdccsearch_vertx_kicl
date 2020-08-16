package com.lxbluem.irc.usecase.requestmodel;

import lombok.Data;

@Data
public class BotMessage {
    private final long timestamp;
    private final String bot;
}
