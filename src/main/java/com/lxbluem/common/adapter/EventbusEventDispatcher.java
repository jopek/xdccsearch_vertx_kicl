package com.lxbluem.common.adapter;

import com.lxbluem.common.domain.events.Event;
import com.lxbluem.common.domain.ports.EventDispatcher;
import com.lxbluem.common.infrastructure.Address;
import com.lxbluem.common.infrastructure.RouteForEventNotFound;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.json.JsonObject;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public class EventbusEventDispatcher implements EventDispatcher {
    private final EventBus eventBus;
    private final Map<Class<? extends Event>, String> routingTable = new HashMap<>();

    public EventbusEventDispatcher(EventBus eventBus) {
        this.eventBus = eventBus;
        Arrays.stream(Address.values())
                .filter(address -> !address.getEventClass().equals(Event.class))
                .forEach(address -> routingTable.put(address.getEventClass(), address.address()));
    }

    @Override
    public <T extends Event> void dispatch(T event) {
        JsonObject message = JsonObject.mapFrom(event);
        String address = Optional.ofNullable(routingTable.get(event.getClass())).orElseThrow(RouteForEventNotFound::new);
        eventBus.publish(address, message);
    }
}
