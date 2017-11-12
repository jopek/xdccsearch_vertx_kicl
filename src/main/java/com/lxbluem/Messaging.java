package com.lxbluem;

import io.vertx.core.json.JsonObject;
import io.vertx.rxjava.core.eventbus.EventBus;
import org.apache.commons.lang3.StringUtils;

import java.time.Instant;

public class Messaging {
    private EventBus eventBus;

    public Messaging(EventBus eventBus) {
        this.eventBus = eventBus;
    }

    public void publishPack(String address, JsonObject pack) {
        publishPack(address, pack, "");
    }

    public void publishPack(String address, JsonObject pack, JsonObject extra) {
        publishPack(address, pack, null, extra);
    }

    public void publishPack(String address, JsonObject pack, Throwable throwable) {
        publishPack(address, pack, throwable.getMessage());
    }

    public void publishPack(String address, JsonObject pack, String message) {
        publishPack(address, pack, message, null);
    }

    private void publishPack(String address, JsonObject pack, String message, JsonObject extra) {
        final JsonObject messageObject = new JsonObject()
                .put("timestamp", Instant.now().toEpochMilli())
                .put("pack", pack);

        if (!StringUtils.isEmpty(message))
            messageObject.put("message", message);

        if (!(extra == null || extra.isEmpty()))
            messageObject.mergeIn(extra);

        eventBus.publish(address, messageObject);
    }

    public void notify(String address, String botName) {
        notify(address, botName, "");
    }

    public void notify(String address, String botName, JsonObject extra) {
        notify(address, botName, null, extra);
    }

    public void notify(String address, String botName, Throwable throwable) {
        notify(address, botName, throwable.getMessage());
    }

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
