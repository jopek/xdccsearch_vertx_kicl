package com.lxbluem.domain.ports;

import io.vertx.core.json.JsonObject;

public interface BotMessaging {
    void notify(String address, String botName);

    void notify(String address, String botName, JsonObject extra);

    void notify(String address, String botName, Throwable throwable);

    void notify(String address, String botName, String message);
}
