package com.lxbluem.irc;

import com.lxbluem.AbstractRouteVerticle;
import com.lxbluem.domain.Pack;
import com.lxbluem.irc.usecase.BotService;
import com.lxbluem.model.SerializedRequest;
import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.json.Json;
import io.vertx.core.json.JsonObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

import static io.vertx.core.http.HttpMethod.DELETE;
import static io.vertx.core.http.HttpMethod.POST;

public class NewBotVerticle extends AbstractRouteVerticle {
    private static final Logger LOG = LoggerFactory.getLogger(NewBotVerticle.class);
    private final BotService botService;

    public NewBotVerticle(BotService botService) {
        this.botService = botService;
    }

    @Override
    public void start(Future<Void> start) {
        promisedRegisterRouteWithHandler(POST, "/xfers", this::startTransfer);
        promisedRegisterRouteWithHandler(DELETE, "/xfers/:botname", this::stopTransfer)
                .setHandler(start);
    }

    private void startTransfer(SerializedRequest serializedRequest, Promise<JsonObject> result) {
        try {
            Pack pack = Json.decodeValue(serializedRequest.getBody(), Pack.class);
            String botNickName = botService.initializeBot(pack);
            result.complete(new JsonObject().put("bot", botNickName));
        } catch (Throwable t) {
            result.fail(t);
        }
    }

    private void stopTransfer(SerializedRequest serializedRequest, Promise<JsonObject> result) {
        Map<String, String> params = serializedRequest.getParams();
        String botNickName = params.get("botname");
        try {
            botService.manualExit(botNickName);
            result.complete(new JsonObject().put("bot", botNickName));
        } catch (Throwable t) {
            result.fail(t);
        }
    }
}
