package com.lxbluem.irc;

import com.lxbluem.common.domain.Pack;
import com.lxbluem.common.domain.ports.BotMessaging;
import com.lxbluem.common.infrastructure.AbstractRouteVerticle;
import com.lxbluem.common.infrastructure.Address;
import com.lxbluem.common.infrastructure.SerializedRequest;
import com.lxbluem.irc.domain.ports.NameGenerator;
import io.vertx.core.Promise;
import io.vertx.core.json.DecodeException;
import io.vertx.core.json.Json;
import io.vertx.core.json.JsonObject;
import io.vertx.rxjava.core.eventbus.EventBus;
import io.vertx.rxjava.core.eventbus.Message;
import org.apache.commons.lang3.StringUtils;
import org.kitteh.irc.client.library.Client;
import org.kitteh.irc.client.library.exception.KittehConnectionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.text.SimpleDateFormat;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static io.vertx.core.http.HttpMethod.*;
import static java.util.stream.Collectors.toMap;

public class BotVerticle extends AbstractRouteVerticle {
    private static final Logger LOG = LoggerFactory.getLogger(BotVerticle.class);

    private final BotMessaging messaging;
    private final Map<Client, Pack> packsByBot = new HashMap<>();

    public BotVerticle(BotMessaging botMessaging) {
        this.messaging = botMessaging;
    }

    @Override
    public void start() {
        EventBus eventBus = vertx.eventBus();

        eventBus.consumer(Address.DCC_FINISHED.address(), this::handleDccFinished);
        eventBus.consumer(Address.BOT_FAILED.address(), this::handleDccFinished);
        eventBus.consumer(Address.BOT_EXITED.address(), this::handleExit);
        registerRoute(POST, "/xfers", this::handleStartTransfer);
        registerRoute(DELETE, "/xfers/:botname", this::handleStopTransfer);
        registerRoute(GET, "/xfers", this::handleListTransfers);
    }

    private void handleExit(Message<JsonObject> message) {
        String botname = message.body().getString("bot", "");
        if (botname == null || botname.isEmpty()) {
            return;
        }
        getClientStream(botname)
                .collect(Collectors.toList())
                .forEach(client -> packsByBot.remove(client));
    }

    private void handleStopTransfer(SerializedRequest serializedRequest, Promise<JsonObject> jsonObjectFuture) {
        String botname = serializedRequest.getParams().get("botname");
        if (botname == null || botname.isEmpty()) {
            jsonObjectFuture.fail("unspecified botname");
            return;
        }

        Optional<Client> first = getClientStream(botname).findFirst();

        if (first.isPresent()) {
            Client client = first.get();
            messaging.notify(Address.BOT_EXITED.address(), botname, "requested shutdown");
            String packNickName = packsByBot.get(client).getNickName();
            client.sendMessage(packNickName, "XDCC CANCEL");
            jsonObjectFuture.complete(new JsonObject().put("bot", client.getNick()));
            vertx.setTimer(2000, e -> client.shutdown());
        } else {
            jsonObjectFuture.fail(String.format("unknown bot '%s'", botname));
        }
    }

    private Stream<Client> getClientStream(String botname) {
        return packsByBot.keySet()
                .stream()
                .filter(pack -> pack.getNick().contentEquals(botname));
    }

    private void handleListTransfers(SerializedRequest serializedRequest, Promise<JsonObject> jsonObjectFuture) {
        Map<String, String> stringMap = packsByBot.entrySet()
                .stream()
                .collect(toMap(
                        k -> k.getKey().getNick(),
                        v -> v.getValue().toString()
                ));

        jsonObjectFuture.complete(JsonObject.mapFrom(stringMap));
    }

    private void handleDccFinished(Message<JsonObject> message) {
        JsonObject body = message.body();
        final String bot = body.getString("bot");

        getClientStream(bot).forEach(ircClient -> {
            String msg = String.format("bot %s exiting because: %s", ircClient.getNick(), body.getString("message", "finished transfer"));
            vertx.setTimer(100, event -> {
                messaging.notify(Address.BOT_EXITED.address(), bot, msg);
                ircClient.shutdown();
            });
        });
    }

    private void handleStartTransfer(SerializedRequest serializedRequest, Promise<JsonObject> jsonObjectFuture) {
        Pack pack;
        try {
            pack = readPackInfo(serializedRequest.getBody());
        } catch (DecodeException e) {
            final String errorMessage = e.getMessage().split("\n")[0];
            jsonObjectFuture.fail(errorMessage);
            return;
        }

        if (pack == null) {
            jsonObjectFuture.fail(new RuntimeException("pack empty"));
            return;
        }

        initializeTransfer(pack, jsonObjectFuture);
    }

    private Pack readPackInfo(String requestBody) {
        if (StringUtils.isEmpty(requestBody)) {
            return null;
        }
        return Json.decodeValue(requestBody, Pack.class);
    }

    private void initializeTransfer(Pack pack, Promise<JsonObject> jsonObjectFuture) {
        String nick = new NameGenerator.RandomNameGenerator().getNick();
        Client client = getClient(pack, nick);

        final String botName = client.getNick();
        client.setExceptionListener(exception -> {
            if (exception instanceof KittehConnectionException) {
                LOG.error("connection cannot be established: {}->{}({}:{}) {}",
                        nick,
                        pack.getNetworkName(),
                        pack.getServerHostName(),
                        pack.getServerPort(),
                        exception.getMessage());
                final JsonObject extra = JsonObject.mapFrom(pack).put("message", exception.getMessage());
                messaging.notify(Address.BOT_FAILED.address(), botName, extra);
            }
        });

        BotEventListener botEventListener = new BotEventListener(messaging, vertx, pack);
        client.getEventManager().registerEventListener(botEventListener);
        client.addChannel(pack.getChannelName());
        client.connect();

        packsByBot.put(client, pack);
        final JsonObject extra = new JsonObject().put("pack", JsonObject.mapFrom(pack));
        messaging.notify(Address.BOT_INITIALIZED.address(), botName, extra);

        jsonObjectFuture.complete(extra.put("bot", botName));
    }

    private Client getClient(Pack pack, String nick) {
        SimpleDateFormat sdf = new SimpleDateFormat("HH:mm:ss");
        return Client.builder()
                .server()
                .host(pack.getServerHostName())
                .port(pack.getServerPort())
                .secure(false)
                .then()
                .nick(nick)
                .name("name_" + nick)
                .user("user_" + nick)
                .realName("realname_" + nick)
//                .inputListener(line -> System.out.println("           " + sdf.format(new Date()) + ' ' + "[I] " + line))
//                .outputListener(line -> System.out.println("           " + sdf.format(new Date()) + ' ' + "[O] " + line))
                .build();
    }


}
