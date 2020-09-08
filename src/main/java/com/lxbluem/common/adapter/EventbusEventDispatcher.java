package com.lxbluem.common.adapter;

import com.lxbluem.common.domain.events.BotEvent;
import com.lxbluem.common.domain.events.DummyEvent;
import com.lxbluem.common.domain.events.Event;
import com.lxbluem.common.domain.ports.EventDispatcher;
import com.lxbluem.common.infrastructure.Address;
import com.lxbluem.common.infrastructure.RouteForEventNotFound;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.json.JsonObject;

import java.time.Clock;
import java.time.Instant;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public class EventbusEventDispatcher implements EventDispatcher {
    private final EventBus eventBus;
    private final Map<Class<? extends Event>, String> routingTable = new HashMap<>();
    private final Clock clock;

    public EventbusEventDispatcher(EventBus eventBus, Clock clock) {
        this.eventBus = eventBus;
        this.clock = clock;
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
        event.setTimestamp(nowEpochMillis());
        JsonObject message = JsonObject.mapFrom(event);
        String routingAddress = routingTable.get(event.getClass());
        String address = Optional.ofNullable(routingAddress)
                .orElseThrow(RouteForEventNotFound::new);
        eventBus.publish(address, message);
    }

    private long nowEpochMillis() {
        return Instant.now(clock).toEpochMilli();
    }
}
