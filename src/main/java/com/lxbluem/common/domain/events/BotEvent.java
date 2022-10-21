package com.lxbluem.common.domain.events;

import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class BotEvent extends Event {
    String bot;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof BotEvent)) return false;
        if (!super.equals(o)) return false;

        BotEvent botEvent = (BotEvent) o;

        return getBot().equals(botEvent.getBot());
    }

    @Override
    public int hashCode() {
        int result = super.hashCode();
        result = 31 * result + getBot().hashCode();
        return result;
    }
}
