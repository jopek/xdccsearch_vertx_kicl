package com.lxbluem.common.domain.events;

import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class BotFailedEvent extends BotEvent {
    private String message;

    @Builder
    public BotFailedEvent(String botNickName, String failMessage) {
        super.bot = botNickName;
        this.message = failMessage;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof BotFailedEvent)) return false;
        if (!super.equals(o)) return false;

        BotFailedEvent that = (BotFailedEvent) o;

        return getMessage().equals(that.getMessage());
    }

    @Override
    public int hashCode() {
        int result = super.hashCode();
        result = 31 * result + getMessage().hashCode();
        return result;
    }
}
