package com.lxbluem.irc.usecase.requestmodel;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class BotMessage {
    private long timestamp;
    private String bot;
}
