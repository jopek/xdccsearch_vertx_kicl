package com.lxbluem;

import io.vertx.core.json.JsonObject;
import org.apache.commons.lang3.StringUtils;
import io.vertx.rxjava.core.eventbus.EventBus;

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
}
