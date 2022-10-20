package com.lxbluem.common.domain.events;

import com.lxbluem.common.domain.Pack;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@EqualsAndHashCode
public class BotInitializedEvent extends BotEvent {
    private Pack pack;

    @Builder
    public BotInitializedEvent(String botNickName, Pack pack) {
        super.bot = botNickName;
        this.pack = pack;
    }
}
