package com.lxbluem.domain.ports;

import com.lxbluem.Address;
import com.lxbluem.irc.usecase.requestmodel.BotMessage;
import io.vertx.core.json.JsonObject;

import java.io.Serializable;
import java.util.Map;
import java.util.function.Consumer;

public interface BotMessaging {
    /**
     * @deprecated instead use {@link BotMessaging#notify(Address, BotMessage)}()}
     */
    @Deprecated
    void notify(String address, String botName);

    /**
     * @deprecated instead use {@link BotMessaging#notify(Address, BotMessage)}()}
     */
    @Deprecated
    void notify(String address, String botName, JsonObject extra);

    /**
     * @deprecated instead use {@link BotMessaging#notify(Address, BotMessage)}()}
     */
    @Deprecated
    void notify(String address, String botName, Throwable throwable);

    /**
     * @deprecated instead use {@link BotMessaging#notify(Address, BotMessage)}()}
     */
    @Deprecated
    void notify(String address, String botName, String message);

    <T extends BotMessage> void notify(Address address, T message);
}
