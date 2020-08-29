package com.lxbluem.common.domain.ports;

import com.lxbluem.common.domain.events.Event;
import com.lxbluem.common.infrastructure.RouteForEventNotFound;

public interface EventDispatcher {
    <T extends Event>void dispatch(T event) throws RouteForEventNotFound;
}
