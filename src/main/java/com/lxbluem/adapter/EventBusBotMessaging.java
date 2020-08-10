package com.lxbluem.adapter;

import com.lxbluem.domain.ports.BotMessaging;
import io.vertx.core.json.JsonObject;
import io.vertx.rxjava.core.eventbus.EventBus;
import org.apache.commons.lang3.StringUtils;

import java.time.Instant;

public class EventBusBotMessaging implements BotMessaging {
    private final EventBus eventBus;

    public EventBusBotMessaging(EventBus eventBus) {
        this.eventBus = eventBus;
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

    private void notify(String address, String botName, String message, JsonObject extra) {
        final JsonObject messageObject = new JsonObject()
                .put("timestamp", Instant.now().toEpochMilli())
                .put("bot", botName);

        if (!StringUtils.isEmpty(message))
            messageObject.put("message", message);

        if (!(extra == null || extra.isEmpty()))
            messageObject.mergeIn(extra);

        eventBus.publish(address, messageObject);
    }
}
