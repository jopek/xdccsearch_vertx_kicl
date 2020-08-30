package com.lxbluem.common.domain.events;

import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class BotDccPackRequestedEvent extends BotEvent {
    private String message;
    private String remoteNickName;
    private int packNumber;

    @Builder
    public BotDccPackRequestedEvent(String bot, long timestamp, String message, String remoteNickName, int packNumber) {
        super(timestamp, bot);
        this.message = message;
        this.remoteNickName = remoteNickName;
        this.packNumber = packNumber;
    }
}
