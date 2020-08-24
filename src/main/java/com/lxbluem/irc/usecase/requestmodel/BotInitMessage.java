package com.lxbluem.irc.usecase.requestmodel;

import com.lxbluem.domain.Pack;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class BotInitMessage extends BotMessage {
    private Pack pack;

    @Builder public BotInitMessage(String bot, long timestamp, Pack pack) {
        super(timestamp, bot);
        this.pack = pack;
    }
}
