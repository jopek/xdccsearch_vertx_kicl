package com.lxbluem.irc;

import com.lxbluem.common.domain.Pack;
import com.lxbluem.common.domain.events.BotFailedEvent;
import com.lxbluem.common.domain.events.DccFinishedEvent;
import com.lxbluem.common.domain.events.DccStartedEvent;
import com.lxbluem.common.infrastructure.AbstractRouteVerticle;
import com.lxbluem.common.infrastructure.Address;
import com.lxbluem.common.infrastructure.SerializedRequest;
import com.lxbluem.irc.domain.model.request.DccFinishedExitCommand;
import com.lxbluem.irc.domain.model.request.InitializeBotCommand;
import com.lxbluem.irc.domain.model.request.RequestedExitCommand;
import com.lxbluem.irc.domain.model.request.ToggleDccTransferStartedCommand;
import com.lxbluem.irc.domain.ports.incoming.ExitBot;
import com.lxbluem.irc.domain.ports.incoming.InitializeBot;
import com.lxbluem.irc.domain.ports.incoming.ToggleDccTransferStarted;
import io.vertx.core.Promise;
import io.vertx.core.json.Json;
import io.vertx.core.json.JsonObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.function.Consumer;

import static com.lxbluem.common.infrastructure.Address.DCC_FAILED;
import static com.lxbluem.common.infrastructure.Address.DCC_FINISHED;
import static com.lxbluem.common.infrastructure.Address.DCC_STARTED;
import static io.vertx.core.http.HttpMethod.DELETE;
import static io.vertx.core.http.HttpMethod.POST;

public class NewBotVerticle extends AbstractRouteVerticle {
    private static final Logger LOG = LoggerFactory.getLogger(NewBotVerticle.class);
    private final InitializeBot initializeBot;
    private final ExitBot exitBot;
    private final ToggleDccTransferStarted toggleDccTransferStarted;

    public NewBotVerticle(
            InitializeBot initializeBot,
            ExitBot exitBot,
            ToggleDccTransferStarted toggleDccTransferStarted) {
        this.initializeBot = initializeBot;
        this.exitBot = exitBot;
        this.toggleDccTransferStarted = toggleDccTransferStarted;
    }

    @Override
    public void start(Promise<Void> start) {
        registerRoute(POST, "/xfers", this::startTransfer);
        registerRoute(DELETE, "/xfers/:botname", this::stopTransfer)
                .onComplete(start);

        handle(DCC_STARTED, this::dccStarted);
        handle(DCC_FINISHED, this::dccFinished);
        handle(DCC_FAILED, this::dccFailed);
    }

    private void startTransfer(SerializedRequest serializedRequest, Promise<JsonObject> result) {
        try {
            Pack pack = Json.decodeValue(serializedRequest.getBody(), Pack.class);
            InitializeBotCommand initializeBotCommand = new InitializeBotCommand(pack);
            String botNickName = initializeBot.handle(initializeBotCommand);
            result.complete(new JsonObject().put("bot", botNickName));
            LOG.debug(">>>>>>>>>");
        } catch (Exception t) {
            result.fail(t);
        }
    }

    private void stopTransfer(SerializedRequest serializedRequest, Promise<JsonObject> result) {
        Map<String, String> params = serializedRequest.getParams();
        String botNickName = params.get("botname");
        try {
            exitBot.handle(new RequestedExitCommand(botNickName));
            result.complete(new JsonObject().put("bot", botNickName));
        } catch (Exception t) {
            result.fail(t);
        }
    }

    private void handle(Address address, Consumer<JsonObject> method) {
        vertx.eventBus()
                .<JsonObject>consumer(address.address(), event -> {
                    try {
                        method.accept(event.body());
                    } catch (Exception t) {
                        event.fail(500, t.getMessage());
                    }
                });
    }

    private void dccStarted(JsonObject eventMessage) {
        DccStartedEvent message = eventMessage.mapTo(DccStartedEvent.class);
        String botNickName = message.getBot();
        ToggleDccTransferStartedCommand command = new ToggleDccTransferStartedCommand(botNickName);
        toggleDccTransferStarted.handle(command);
    }

    private void dccFailed(JsonObject eventMessage) {
        BotFailedEvent message = eventMessage.mapTo(BotFailedEvent.class);
        exitBot.handle(new DccFinishedExitCommand(message.getBot(), message.getMessage()));
    }

    private void dccFinished(JsonObject eventMessage) {
        DccFinishedEvent message = eventMessage.mapTo(DccFinishedEvent.class);
        exitBot.handle(new DccFinishedExitCommand(message.getBot(), "DCC transfer finished"));
    }

}
