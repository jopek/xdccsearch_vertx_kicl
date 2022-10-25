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
    public BotDccPackRequestedEvent(String botNickName, String message, String remoteNickName, int packNumber) {
        super.bot = botNickName;
        this.message = message;
        this.remoteNickName = remoteNickName;
        this.packNumber = packNumber;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof BotDccPackRequestedEvent that)) return false;
        if (!super.equals(o)) return false;

        if (getPackNumber() != that.getPackNumber()) return false;
        if (!getMessage().equals(that.getMessage())) return false;
        return getRemoteNickName().equals(that.getRemoteNickName());
    }

    @Override
    public int hashCode() {
        int result = super.hashCode();
        result = 31 * result + getMessage().hashCode();
        result = 31 * result + getRemoteNickName().hashCode();
        result = 31 * result + getPackNumber();
        return result;
    }
}
