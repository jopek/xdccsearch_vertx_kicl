package com.lxbluem.common.adapter;

import com.lxbluem.common.domain.events.BotExitedEvent;
import com.lxbluem.common.domain.events.Event;
import com.lxbluem.common.domain.ports.EventDispatcher;
import com.lxbluem.common.infrastructure.RouteForEventNotFound;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.json.JsonObject;
import org.junit.Before;
import org.junit.Test;

import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.*;

public class EventbusEventDispatcherTest {

    EventBus eventBus;
    EventDispatcher uut;

    @Before
    public void setUp() {
        eventBus = mock(EventBus.class);
        uut = new EventbusEventDispatcher(eventBus);
    }

    @Test
    public void route_event_to_resolved_address() {
        uut.dispatch(new BotExitedEvent("name", 9L, "ey"));

        JsonObject expected = new JsonObject()
                .put("timestamp", 9L)
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