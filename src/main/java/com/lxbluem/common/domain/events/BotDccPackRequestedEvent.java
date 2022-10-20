package com.lxbluem.common.domain.events;

import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@EqualsAndHashCode
public class BotDccPackRequestedEvent extends BotEvent {
    private String message;
    private String remoteNickName;
    private int packNumber;

    @Builder
    public BotDccPackRequestedEvent(String botNickName, String message, String remoteNickName, int packNumber) {
        super.bot = botNickName;
        this.message = message;
        this.remoteNickName = remoteNickName;
        this.packNumber = packNumber;
    }
}
