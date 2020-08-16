package com.lxbluem.domain.ports;

import com.lxbluem.irc.usecase.requestmodel.BotMessage;
import io.vertx.core.json.JsonObject;
import io.vertx.core.json.jackson.DatabindCodec;

public interface BotMessaging {
    /**
     * @deprecated instead use {@link BotMessaging#notify(BotMessage)}()}
     */
    @Deprecated
    void notify(String address, String botName);

    /**
     * @deprecated instead use {@link BotMessaging#notify(BotMessage)}()}
     */
    @Deprecated
    void notify(String address, String botName, JsonObject extra);

    /**
     * @deprecated instead use {@link BotMessaging#notify(BotMessage)}()}
     */
    @Deprecated
    void notify(String address, String botName, Throwable throwable);

    /**
     * @deprecated instead use {@link BotMessaging#notify(BotMessage)}()}
     */
    @Deprecated
    void notify(String address, String botName, String message);

    <T extends BotMessage> void notify(T message);
}
