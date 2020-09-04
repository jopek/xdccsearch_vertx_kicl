package com.lxbluem.irc;

import com.lxbluem.common.domain.Pack;
import com.lxbluem.common.domain.events.BotFailedEvent;
import com.lxbluem.common.domain.events.DccFinishedEvent;
import com.lxbluem.common.infrastructure.AbstractRouteVerticle;
import com.lxbluem.common.infrastructure.Address;
import com.lxbluem.common.infrastructure.SerializedRequest;
import com.lxbluem.irc.domain.BotService;
import com.lxbluem.irc.domain.model.request.InitializeBotCommand;
import com.lxbluem.irc.domain.ports.incoming.InitializeBot;
import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.json.Json;
import io.vertx.core.json.JsonObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import rx.functions.Action1;

import java.util.Map;

import static com.lxbluem.common.infrastructure.Address.DCC_FAILED;
import static com.lxbluem.common.infrastructure.Address.DCC_FINISHED;
import static io.vertx.core.http.HttpMethod.DELETE;
import static io.vertx.core.http.HttpMethod.POST;

public class NewBotVerticle extends AbstractRouteVerticle {
    private static final Logger LOG = LoggerFactory.getLogger(NewBotVerticle.class);
    private final InitializeBot initializeBot;
    private final BotService botService;

    public NewBotVerticle(InitializeBot initializeBot, BotService botService) {
        this.initializeBot = initializeBot;
        this.botService = botService;
    }

    @Override
    public void start(Future<Void> start) {
        registerRoute(POST, "/xfers", this::startTransfer);
        registerRoute(DELETE, "/xfers/:botname", this::stopTransfer)
                .setHandler(start);

        handle(DCC_FINISHED, this::dccFinished);
        handle(DCC_FAILED, this::dccFailed);
    }

    private void startTransfer(SerializedRequest serializedRequest, Promise<JsonObject> result) {
        try {
            Pack pack = Json.decodeValue(serializedRequest.getBody(), Pack.class);
            InitializeBotCommand initializeBotCommand = new InitializeBotCommand(pack);
            String botNickName = initializeBot.handle(initializeBotCommand);
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

    private void handle(Address address, Action1<JsonObject> method) {
        vertx.eventBus()
                .<JsonObject>consumer(address.address(), event -> {
                    try {
                        method.call(event.body());
                    } catch (Throwable t) {
                        event.fail(500, t.getMessage());
                    }
                });
    }

    private void dccFailed(JsonObject eventMessage) {
        BotFailedEvent message = eventMessage.mapTo(BotFailedEvent.class);
        botService.exit(message.getBot(), message.getMessage());
    }

    private void dccFinished(JsonObject eventMessage) {
        DccFinishedEvent message = eventMessage.mapTo(DccFinishedEvent.class);
        botService.exit(message.getBot(), "DCC transfer finished");
    }

}
