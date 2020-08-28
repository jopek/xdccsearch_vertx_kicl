package com.lxbluem.common.adapter;

import com.lxbluem.common.domain.events.BotEvent;
import com.lxbluem.common.domain.events.DummyEvent;
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
                .filter(
                        address -> !Arrays.asList(
                                Event.class,
                                BotEvent.class,
                                DummyEvent.class
                        ).contains(address.getEventClass())
                )
                .forEach(address -> routingTable.put(address.getEventClass(), address.address()));
    }

    @Override
    public <T extends Event> void dispatch(T event) {
        JsonObject message = JsonObject.mapFrom(event);
        String routingAddress = routingTable.get(event.getClass());
        String address = Optional.ofNullable(routingAddress)
                .orElseThrow(RouteForEventNotFound::new);
        eventBus.publish(address, message);
    }
}
