package com.lxbluem.common.adapter;

import com.lxbluem.common.domain.events.BotExitedEvent;
import com.lxbluem.common.domain.events.Event;
import com.lxbluem.common.domain.ports.EventDispatcher;
import com.lxbluem.common.infrastructure.RouteForEventNotFound;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.json.JsonObject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

class EventbusEventDispatcherTest {

    private EventBus eventBus;
    private EventDispatcher uut;
    private final Instant fixedInstant = Instant.parse("2020-08-10T10:11:22Z");

    @BeforeEach
    void setUp() {
        eventBus = mock(EventBus.class);
        Clock clock = Clock.fixed(fixedInstant, ZoneId.systemDefault());
        uut = new EventbusEventDispatcher(eventBus, clock);
    }

    @Test
    void route_event_to_resolved_address() {
        uut.dispatch(new BotExitedEvent("name", "ey"));

        JsonObject expected = new JsonObject()
                .put("timestamp", 1597054282000L)
                .put("bot", "name")
                .put("message", "ey");
        verify(eventBus).publish("bot.exit", expected);
    }

    @Test
    void throws_exception_if_event_class_not_registered() {
        Event event = new Event();
        assertThrows(RouteForEventNotFound.class, () -> uut.dispatch(event));
        verifyNoMoreInteractions(eventBus);
    }
}
