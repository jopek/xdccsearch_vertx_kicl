package com.lxbluem.common.adapter;

import com.lxbluem.common.domain.events.BotExitedEvent;
import com.lxbluem.common.domain.events.Event;
import com.lxbluem.common.domain.ports.EventDispatcher;
import com.lxbluem.common.infrastructure.RouteForEventNotFound;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.json.JsonObject;
import org.junit.Before;
import org.junit.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;

import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.*;

public class EventbusEventDispatcherTest {

    private EventBus eventBus;
    private EventDispatcher uut;
    private final Instant fixedInstant = Instant.parse("2020-08-10T10:11:22Z");

    @Before
    public void setUp() {
        eventBus = mock(EventBus.class);
        Clock clock = Clock.fixed(fixedInstant, ZoneId.systemDefault());
        uut = new EventbusEventDispatcher(eventBus, clock);
    }

    @Test
    public void route_event_to_resolved_address() {
        uut.dispatch(new BotExitedEvent("name", "ey"));

        JsonObject expected = new JsonObject()
                .put("timestamp", 1597054282000L)
                .put("bot", "name")
                .put("message", "ey");
        verify(eventBus).publish(eq("bot.exit"), eq(expected));
    }

    @Test(expected = RouteForEventNotFound.class)
    public void throws_exception_if_event_class_not_registered() {
        uut.dispatch(new Event());
        verifyNoMoreInteractions(eventBus);
    }
}