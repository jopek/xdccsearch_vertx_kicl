package com.lxbluem.common.domain.events;

import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class BotExitedEvent extends BotEvent {
    private String message;

    @Builder
    public BotExitedEvent(String botNickName, String message) {
        super.bot = botNickName;
        this.message = message;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof BotExitedEvent that)) return false;
        if (!super.equals(o)) return false;

        return getMessage().equals(that.getMessage());
    }

    @Override
    public int hashCode() {
        int result = super.hashCode();
        result = 31 * result + getMessage().hashCode();
        return result;
    }
}
