package com.lxbluem.common.domain.events;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@EqualsAndHashCode
public class BotEvent extends Event {
    String bot;
}
