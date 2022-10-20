package com.lxbluem.common.domain.ports;

import com.lxbluem.common.domain.events.Event;
import com.lxbluem.common.infrastructure.Address;
import io.vertx.core.json.JsonObject;

import java.io.Serializable;
import java.util.Map;
import java.util.function.Consumer;

public interface BotMessaging {
    /**
     * @deprecated instead use {@link BotMessaging#notify(Address, Event)}()}
     */
    @Deprecated()
    void notify(String address, String botName);

    /**
     * @deprecated instead use {@link BotMessaging#notify(Address, Event)}()}
     */
    @Deprecated
    void notify(String address, String botName, JsonObject extra);

    /**
     * @deprecated instead use {@link BotMessaging#notify(Address, Event)}()}
     */
    @Deprecated
    void notify(String address, String botName, Throwable throwable);

    /**
     * @deprecated instead use {@link BotMessaging#notify(Address, Event)}()}
     */
    @Deprecated
    void notify(String address, String botName, String message);

    <T extends Event> void notify(Address address, T message);

    <T extends Serializable> void ask(Address address, T message, Consumer<Map<String, Object>> answerHandler);

    void notify(Address address, Object message);
}
