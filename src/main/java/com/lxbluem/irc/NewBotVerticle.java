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

import static io.vertx.core.http.HttpMethod.POST;

public class NewBotVerticle extends AbstractRouteVerticle {
    private static final Logger LOG = LoggerFactory.getLogger(NewBotVerticle.class);
    private final BotService botService;

    public NewBotVerticle(BotService botService) {
        this.botService = botService;
    }

    @Override
    public void start(Future<Void> start) {
        promisedRegisterRouteWithHandler(POST, "/v2/xfers", this::handleStartTransfer)
                .setHandler(start);
    }

    private void startTransfer(SerializedRequest serializedRequest, Promise<JsonObject> result) {
        try {
            Pack pack = Json.decodeValue(serializedRequest.getBody(), Pack.class);
            String botNick = botService.initializeBot(pack);
            result.complete(new JsonObject().put("bot", botNick));
        } catch (Throwable t) {
            result.fail(t);
        }
    }

    private Pack readPackInfo(String requestBody) {
        if (StringUtils.isEmpty(requestBody)) {
            return null;
        }
        try {
            return Json.decodeValue(requestBody, Pack.class);
        } catch (DecodeException e) {
            LOG.error("error decoding String -> Pack", e);
            return null;
        }
    }
}
