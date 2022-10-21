package com.lxbluem.common.domain.events;

import com.lxbluem.common.domain.Pack;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class BotInitializedEvent extends BotEvent {
    private Pack pack;

    @Builder
    public BotInitializedEvent(String botNickName, Pack pack) {
        super.bot = botNickName;
        this.pack = pack;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof BotInitializedEvent)) return false;
        if (!super.equals(o)) return false;

        BotInitializedEvent that = (BotInitializedEvent) o;

        return getPack().equals(that.getPack());
    }

    @Override
    public int hashCode() {
        int result = super.hashCode();
        result = 31 * result + getPack().hashCode();
        return result;
    }
}
