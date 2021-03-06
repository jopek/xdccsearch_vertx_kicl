package com.lxbluem.common.adapter;

import com.lxbluem.common.domain.events.Event;
import com.lxbluem.common.domain.ports.BotMessaging;
import com.lxbluem.common.infrastructure.Address;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.json.JsonObject;
import org.apache.commons.lang3.StringUtils;

import java.io.Serializable;
import java.time.Clock;
import java.time.Instant;
import java.util.Map;
import java.util.function.Consumer;

public class EventBusBotMessaging implements BotMessaging {
    private final EventBus eventBus;
    private final Clock clock;

    public EventBusBotMessaging(EventBus eventBus, Clock clock) {
        this.eventBus = eventBus;
        this.clock = clock;
    }

    @Override
    public void notify(String address, String botName) {
        notify(address, botName, "");
    }

    @Override
    public void notify(String address, String botName, JsonObject extra) {
        notify(address, botName, null, extra);
    }

    @Override
    public void notify(String address, String botName, Throwable throwable) {
        notify(address, botName, throwable.getMessage());
    }

    @Override
    public void notify(String address, String botName, String message) {
        notify(address, botName, message, null);
    }

    @Override
    public <T extends Event> void notify(Address address, T message) {
        JsonObject messageObject = JsonObject.mapFrom(message);
        publish(address.address(), messageObject);
    }

    private void notify(String address, String botName, String message, JsonObject extra) {
        final JsonObject messageObject = new JsonObject()
                .put("timestamp", Instant.now(clock).toEpochMilli())
                .put("bot", botName);

        if (!StringUtils.isEmpty(message))
            messageObject.put("message", message);

        if (!(extra == null || extra.isEmpty()))
            messageObject.mergeIn(extra);

        publish(address, messageObject);
    }

    @Override
    public <T extends Serializable> void ask(Address address, T message, Consumer<Map<String, Object>> answerHandler) {
        JsonObject messageObject = JsonObject.mapFrom(message);
        eventBus.<JsonObject>request(address.address(), messageObject, event -> {
            if (event.succeeded()) {
                answerHandler.accept(event.result().body().getMap());
            }
        });

    }

    @Override
    public void notify(Address address, Object message) {
        JsonObject messageObject = JsonObject.mapFrom(message);
        publish(address.address(), messageObject);
    }

    private void publish(String address, JsonObject messageObject) {
        eventBus.publish(address, messageObject);
    }
}
