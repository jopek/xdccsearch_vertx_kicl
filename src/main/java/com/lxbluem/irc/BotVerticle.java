package com.lxbluem.irc;

import com.lxbluem.AbstractRouteVerticle;
import com.lxbluem.Messaging;
import com.lxbluem.model.Pack;
import com.lxbluem.model.SerializedRequest;
import io.vertx.core.Future;
import io.vertx.core.json.DecodeException;
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

import static com.lxbluem.Addresses.*;
import static io.vertx.core.http.HttpMethod.GET;
import static io.vertx.core.http.HttpMethod.POST;
import static java.util.stream.Collectors.toMap;

public class BotVerticle extends AbstractRouteVerticle {
    private static final Logger LOG = LoggerFactory.getLogger(BotVerticle.class);

    private Messaging messaging;
    private Map<Client, Pack> packsByBot = new HashMap<>();

    @Override
    public void start() {
        EventBus eventBus = vertx.eventBus();

        eventBus.consumer(BOT_DCC_FINISH, this::handleDccFinished);
        eventBus.consumer(BOT_FAIL, this::handleDccFinished);
        registerRouteWithHandler(POST, "/xfers", this::handleStartTransfer);
        registerRouteWithHandler(GET, "/xfers", this::handleListTransfers);

        messaging = new Messaging(eventBus);
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
        final String bot = body.getString("bot");
        MutableObject<Client> mutableClientObject = new MutableObject<>();

        packsByBot.entrySet()
                .stream()
                .filter(clientPackEntry -> clientPackEntry.getKey().getNick().equalsIgnoreCase(bot))
                .map(Map.Entry::getKey)
                .forEach(ircClient -> {
                    String msg = String.format("bot %s exiting because: %s", ircClient.getNick(), body.getString("message"));
                    vertx.setTimer(5000, event -> {
                        messaging.notify(BOT_EXIT, bot, msg);
                        ircClient.shutdown();
                        packsByBot.remove(mutableClientObject.getValue());
                    });
                    mutableClientObject.setValue(ircClient);
                });
    }

    private void handleStartTransfer(SerializedRequest serializedRequest, Future<JsonObject> jsonObjectFuture) {
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

        initTx(pack);
    }

    private Pack readPackInfo(String requestBody) {
        if (StringUtils.isEmpty(requestBody)) {
            return null;
        }
        return Json.decodeValue(requestBody, Pack.class);
    }

    private void initTx(Pack pack) {
        String nick = getRandomNick();
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
                messaging.notify(BOT_FAIL, botName, extra);
            }
        });

        BotEventListener botEventListener = new BotEventListener(vertx, pack);
        client.getEventManager().registerEventListener(botEventListener);
        client.addChannel(pack.getChannelName());

        packsByBot.put(client, pack);
        messaging.notify(BOT_INIT, botName, new JsonObject().put("pack", JsonObject.mapFrom(pack)));
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
