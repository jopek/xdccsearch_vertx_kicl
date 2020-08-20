package com.lxbluem.irc;

import com.lxbluem.AbstractRouteVerticle;
import com.lxbluem.domain.Pack;
import com.lxbluem.domain.ports.BotMessaging;
import com.lxbluem.irc.usecase.BotManagementService;
import com.lxbluem.model.SerializedRequest;
import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.json.DecodeException;
import io.vertx.core.json.Json;
import io.vertx.core.json.JsonObject;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static io.vertx.core.http.HttpMethod.POST;

public class NewBotVerticle extends AbstractRouteVerticle {
    private static final Logger LOG = LoggerFactory.getLogger(NewBotVerticle.class);

    private final BotMessaging messaging;
    private final BotManagementService managementService;


    public NewBotVerticle(
            BotMessaging botMessaging,
            BotManagementService managementService
    ) {
        this.messaging = botMessaging;
        this.managementService = managementService;
    }

    @Override
    public void start(Future<Void> start) {
        promisedRegisterRouteWithHandler(POST, "/v2/xfers", this::handleStartTransfer)
                .setHandler(start);
    }

    private void handleStartTransfer(SerializedRequest serializedRequest, Promise<JsonObject> result) {
        Pack pack = readPackInfo(serializedRequest.getBody());
        if (pack == null) {
            result.fail(new RuntimeException("pack empty"));
            return;
        }
        String botNick = managementService.startTransferOf(pack);
        result.complete(new JsonObject().put("bot", botNick));
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
