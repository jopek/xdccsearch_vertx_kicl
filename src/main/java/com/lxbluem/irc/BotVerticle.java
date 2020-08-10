package com.lxbluem.irc;

import com.lxbluem.AbstractRouteVerticle;
import com.lxbluem.domain.ports.BotMessaging;
import com.lxbluem.domain.Pack;
import com.lxbluem.model.SerializedRequest;
import io.vertx.core.Future;
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

import static com.lxbluem.Addresses.BOT_DCC_FINISH;
import static com.lxbluem.Addresses.BOT_EXIT;
import static com.lxbluem.Addresses.BOT_FAIL;
import static com.lxbluem.Addresses.BOT_INIT;
import static io.vertx.core.http.HttpMethod.DELETE;
import static io.vertx.core.http.HttpMethod.GET;
import static io.vertx.core.http.HttpMethod.POST;
import static java.util.stream.Collectors.toMap;

public class BotVerticle extends AbstractRouteVerticle {
    private static final Logger LOG = LoggerFactory.getLogger(BotVerticle.class);

    private BotMessaging messaging;
    private Map<Client, Pack> packsByBot = new HashMap<>();

    public BotVerticle(BotMessaging botMessaging) {
        this.messaging = botMessaging;
    }

    @Override
    public void start() {
        EventBus eventBus = vertx.eventBus();

        eventBus.consumer(BOT_DCC_FINISH, this::handleDccFinished);
        eventBus.consumer(BOT_FAIL, this::handleDccFinished);
        eventBus.consumer(BOT_EXIT, this::handleExit);
        registerRouteWithHandler(POST, "/xfers", this::handleStartTransfer);
        registerRouteWithHandler(DELETE, "/xfers/:botname", this::handleStopTransfer);
        registerRouteWithHandler(GET, "/xfers", this::handleListTransfers);
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

    private void handleStopTransfer(SerializedRequest serializedRequest, Future<JsonObject> jsonObjectFuture) {
        String botname = serializedRequest.getParams().get("botname");
        if (botname == null || botname.isEmpty()) {
            jsonObjectFuture.fail("unspecified botname");
            return;
        }

        Optional<Client> first = getClientStream(botname).findFirst();

        if (first.isPresent()) {
            Client client = first.get();
            messaging.notify(BOT_EXIT, botname, "requested shutdown");
            String packNickName = packsByBot.get(client).getNickName();
            client.sendMessage(packNickName, "XDCC CANCEL");
            jsonObjectFuture.complete(new JsonObject().put("bot", client.getNick()));
            vertx.setTimer(2000, e -> client.shutdown());
        } else {
            jsonObjectFuture.fail(String.format("unknown bot '%s'", botname));
        }
    }

    private Stream<Client> getClientStream(String botname) {
        return packsByBot.entrySet()
                .stream()
                .filter(clientPackEntry -> clientPackEntry.getKey().getNick().contentEquals(botname))
                .map(Map.Entry::getKey);
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

        getClientStream(bot).forEach(ircClient -> {
            String msg = String.format("bot %s exiting because: %s", ircClient.getNick(), body.getString("message", "finished transfer"));
            vertx.setTimer(100, event -> {
                messaging.notify(BOT_EXIT, bot, msg);
                ircClient.shutdown();
            });
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

        initializeTransfer(pack, jsonObjectFuture);
    }

    private Pack readPackInfo(String requestBody) {
        if (StringUtils.isEmpty(requestBody)) {
            return null;
        }
        return Json.decodeValue(requestBody, Pack.class);
    }

    private void initializeTransfer(Pack pack, Future<JsonObject> jsonObjectFuture) {
        String nick = NameGenerator.getRandomNick();
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

        BotEventListener botEventListener = new BotEventListener(messaging, vertx, pack);
        client.getEventManager().registerEventListener(botEventListener);
        client.addChannel(pack.getChannelName());
        client.connect();

        packsByBot.put(client, pack);
        final JsonObject extra = new JsonObject().put("pack", JsonObject.mapFrom(pack));
        messaging.notify(BOT_INIT, botName, extra);

        jsonObjectFuture.complete(extra.put("bot", botName));
    }

    private Client getClient(Pack pack, String nick) {
        SimpleDateFormat sdf = new SimpleDateFormat("HH:mm:ss");
        return Client.builder()
                .serverHost(pack.getServerHostName())
                .serverPort(pack.getServerPort())
                .nick(nick)
                .name("name_" + nick)
                .user("user_" + nick)
                .realName("realname_" + nick)
                .secure(false)
//                .inputListener(line -> System.out.println("           " + sdf.format(new Date()) + ' ' + "[I] " + line))
//                .outputListener(line -> System.out.println("           " + sdf.format(new Date()) + ' ' + "[O] " + line))
                .build();
    }


}
