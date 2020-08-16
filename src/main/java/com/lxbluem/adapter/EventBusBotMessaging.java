package com.lxbluem.adapter;

import com.lxbluem.domain.ports.BotMessaging;
import com.lxbluem.irc.usecase.requestmodel.BotMessage;
import io.vertx.core.json.JsonObject;
import io.vertx.rxjava.core.eventbus.EventBus;
import org.apache.commons.lang3.StringUtils;

import java.time.Clock;
import java.time.Instant;
import java.util.Map;

public class EventBusBotMessaging implements BotMessaging {
    private final EventBus eventBus;
    private final Clock clock;
    private final Map<Class<? extends BotMessage>, String> messageToAddressMap;

    public EventBusBotMessaging(EventBus eventBus, Clock clock, Map<Class<? extends BotMessage>, String> messageToAddressMap) {
        this.eventBus = eventBus;
        this.clock = clock;
        this.messageToAddressMap = messageToAddressMap;
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
    public <T extends BotMessage> void notify(T message) {
        JsonObject messageObject = JsonObject.mapFrom(message);
        String address = messageToAddressMap.get(message.getClass());
        publish(address, messageObject);
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

    private void publish(String address, JsonObject messageObject) {
        eventBus.publish(address, messageObject);
    }
}
