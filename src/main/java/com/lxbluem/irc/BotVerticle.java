package com.lxbluem.irc;

import com.lxbluem.AbstractRouteVerticle;
import com.lxbluem.model.Pack;
import com.lxbluem.model.SerializedRequest;
import io.vertx.core.Future;
import io.vertx.core.json.Json;
import io.vertx.core.json.JsonObject;
import io.vertx.rxjava.core.eventbus.EventBus;
import io.vertx.rxjava.core.eventbus.Message;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.mutable.MutableObject;
import org.kitteh.irc.client.library.Client;
import org.kitteh.irc.client.library.exception.KittehConnectionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.text.SimpleDateFormat;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

import static io.vertx.core.http.HttpMethod.GET;
import static io.vertx.core.http.HttpMethod.POST;
import static java.util.stream.Collectors.toMap;

public class BotVerticle extends AbstractRouteVerticle {
    private static final Logger LOG = LoggerFactory.getLogger(BotVerticle.class);

    private EventBus eventBus;
    private Map<Client, Pack> packsByBot = new HashMap<>();

    @Override
    public void start() {
        eventBus = vertx.eventBus();

        eventBus.consumer("bot.dcc.finish", this::handleDccFinished);
        eventBus.consumer("bot.dcc.fail", this::handleDccFinished);
        registerRouteWithHandler(POST, "/xfers", this::handleStartTransfer);
        registerRouteWithHandler(GET, "/xfers", this::handleListTransfers);
    }

    private void handleListTransfers(SerializedRequest serializedRequest, Future<JsonObject> jsonObjectFuture) {
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
        MutableObject<Client> mutableClientObject = new MutableObject<>();

        packsByBot.forEach((ircClient, pack) -> {
            long packId = pack.getPackId();
            long mappedPackId = body.getJsonObject("pack").getLong("pid");
            if (packId == mappedPackId) {
                vertx.setTimer(5000, event -> {
                    ircClient.shutdown();
                    packsByBot.remove(mutableClientObject.getValue());
                });
                mutableClientObject.setValue(ircClient);
            }
        });
    }

    private void handleStartTransfer(SerializedRequest serializedRequest, Future<JsonObject> jsonObjectFuture) {
        Pack pack = readPackInfo(serializedRequest, jsonObjectFuture);

        if (pack == null)
            return;

        initTx(pack);
    }

    private Pack readPackInfo(SerializedRequest request, Future<JsonObject> future) {
        String requestBody = request.getBody();
        if (StringUtils.isEmpty(requestBody)) {
            future.complete(new JsonObject());
            return null;
        }

        Pack pack = Json.decodeValue(requestBody, Pack.class);
        future.complete(JsonObject.mapFrom(pack));
        return pack;
    }

    private void initTx(Pack pack) {
        String nick = getRandomNick();
        Client client = getClient(pack, nick);

        client.setExceptionListener(e -> {
            if (e instanceof KittehConnectionException) {
                LOG.error("connection cannot be established: {}->{}({}:{}) {}",
                        nick,
                        pack.getNetworkName(),
                        pack.getServerHostName(),
                        pack.getServerPort(),
                        e.getMessage());
                client.shutdown();

                eventBus.publish("bot.fail", new JsonObject()
                        .put("message", e.getMessage())
                        .put("pack", JsonObject.mapFrom(pack)));
            }
        });

        BotEventListener botEventListener = new BotEventListener(vertx, pack);
        client.getEventManager().registerEventListener(botEventListener);
        client.addChannel(pack.getChannelName());

        packsByBot.put(client, pack);

        eventBus.publish("bot.init", new JsonObject()
                .put("pack", JsonObject.mapFrom(pack)));

    }

    private Client getClient(Pack pack, String nick) {
        SimpleDateFormat sdf = new SimpleDateFormat("HH:mm:ss");
        return Client.builder()
                .serverHost(pack.getServerHostName())
                .serverPort(pack.getServerPort())
                .nick("nick_" + nick)
                .name("name_" + nick)
                .user("user_" + nick)
                .realName("realname_" + nick)
                .secure(false)
//                .listenInput(line -> System.out.println("           " + sdf.format(new Date()) + ' ' + "[I] " + line))
//                .listenOutput(line -> System.out.println("           " + sdf.format(new Date()) + ' ' + "[O] " + line))
                .build();
    }

    private String getRandomNick() {
        StringBuilder stringBuilder = new StringBuilder();
        Random random = new Random();
        String dictionary = "abcdefghijklmnopqrstuvwxyz0123456789";
        for (int i = 0; i < 4; i++) {
            stringBuilder.append(
                    dictionary.charAt(
                            random.nextInt(
                                    dictionary.length()
                            )
                    )
            );
        }
        return stringBuilder.toString();
    }

}
