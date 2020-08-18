package com.lxbluem.irc.usecase.requestmodel;

import com.lxbluem.domain.Pack;
import lombok.Builder;
import lombok.Getter;

@Getter
public class BotInitMessage extends BotMessage {
    private final Pack pack;

    @Builder public BotInitMessage(String bot, long timestamp, Pack pack) {
        super(timestamp, bot);
        this.pack = pack;
    }
}
