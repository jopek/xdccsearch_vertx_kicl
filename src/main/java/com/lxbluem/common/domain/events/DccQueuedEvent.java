package com.lxbluem.common.domain.events;

import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class DccQueuedEvent extends BotEvent {
    private String message;

    @Builder
    public DccQueuedEvent(String botNickName, String noticeMessage) {
        super.bot = botNickName;
        message = noticeMessage;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof DccQueuedEvent)) return false;
        if (!super.equals(o)) return false;

        DccQueuedEvent that = (DccQueuedEvent) o;

        return getMessage().equals(that.getMessage());
    }

    @Override
    public int hashCode() {
        int result = super.hashCode();
        result = 31 * result + getMessage().hashCode();
        return result;
    }
}
