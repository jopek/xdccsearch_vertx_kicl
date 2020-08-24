package com.lxbluem.irc;

import com.lxbluem.AbstractRouteVerticle;
import com.lxbluem.domain.Pack;
import com.lxbluem.irc.usecase.BotService;
import com.lxbluem.irc.usecase.requestmodel.BotDccFinishedMessage;
import com.lxbluem.irc.usecase.requestmodel.BotFailMessage;
import com.lxbluem.model.SerializedRequest;
import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.json.Json;
import io.vertx.core.json.JsonObject;
import io.vertx.rxjava.core.eventbus.Message;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import rx.functions.Action1;

import java.util.Map;

import static com.lxbluem.Addresses.BOT_DCC_FINISH;
import static com.lxbluem.Addresses.BOT_FAIL;
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

        handle(BOT_DCC_FINISH, this::dccFinished);
        handle(BOT_FAIL, this::botFailed);
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

    private void handle(String address, Action1<JsonObject> method) {
        vertx.eventBus()
                .<JsonObject>consumer(address)
                .toObservable()
                .map(Message::body)
                .subscribe(method);
    }

    private void dccFinished(JsonObject eventMessage) {
        BotDccFinishedMessage message = eventMessage.mapTo(BotDccFinishedMessage.class);
        botService.exit(message.getBot(), "DCC transfer finished");
    }

    private void botFailed(JsonObject eventMessage) {
        BotFailMessage message = eventMessage.mapTo(BotFailMessage.class);
        botService.exit(message.getBot(), message.getMessage());
    }
}
