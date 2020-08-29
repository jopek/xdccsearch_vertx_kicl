package com.lxbluem.common.domain.events;

import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class DummyEvent extends Event {
    public DummyEvent(long timestamp) {
        super(timestamp);
    }
}
