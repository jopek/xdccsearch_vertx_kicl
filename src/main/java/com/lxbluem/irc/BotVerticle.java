package com.lxbluem.irc;

import com.lxbluem.AbstractRouteVerticle;
import com.lxbluem.model.Pack;
import com.lxbluem.model.SerializedRequest;
import io.vertx.core.Future;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.eventbus.Message;
import io.vertx.core.json.Json;
import io.vertx.core.json.JsonObject;
import net.engio.mbassy.listener.Handler;
import org.kitteh.irc.client.library.Client;
import org.kitteh.irc.client.library.event.channel.ChannelJoinEvent;
import org.kitteh.irc.client.library.event.channel.ChannelMessageEvent;
import org.kitteh.irc.client.library.event.client.NickRejectedEvent;
import org.kitteh.irc.client.library.event.user.PrivateMessageEvent;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import static io.vertx.core.http.HttpMethod.POST;

public class BotVerticle extends AbstractRouteVerticle {
  private static final String ADDRESS = "bot";
  private EventBus eventBus;

  private Map<Client, Pack> packsByBot = new HashMap<>();

  @Override
  public void start() {
    eventBus = vertx.eventBus();

    eventBus.consumer("bot.start", this::parseBotStartMessage);

    registerRouteWithHandler(getClass().getSimpleName(), "POST", "/xfers", this::readPackInfo);
  }

  private void parseBotStartMessage(Message<Object> objectMessage) {
    JsonObject body = (JsonObject) objectMessage.body();
    String nick = body.getString("nick");
  }

  private void readPackInfo(SerializedRequest request, Future<JsonObject> future) {
    Pack pack = Json.decodeValue(request.getBody(), Pack.class);
    future.complete(JsonObject.mapFrom(pack));
    initTx(pack);
  }

  private void initTx(Pack pack) {
    SimpleDateFormat sdf = new SimpleDateFormat("HH:mm:ss");
    Client client = Client.builder()
        .serverHost(pack.getServerHostName())
        .serverPort(pack.getServerPort())
        .secure(false)
        .listenInput(line -> System.out.println(sdf.format(new Date()) + ' ' + "[I] " + line))
        .listenOutput(line -> System.out.println(sdf.format(new Date()) + ' ' + "[O] " + line))
        .listenException(Throwable::printStackTrace)
        .build();

    client.getEventManager().registerEventListener(this);

    client.addChannel(pack.getChannelName());

    packsByBot.putIfAbsent(client, pack);
  }

  @Handler
  public void onJoin(ChannelJoinEvent event) {
    System.out.println(event);

    StringBuilder buf = new StringBuilder();
    event.getAffectedChannel().ifPresent(buf::append);
    JsonObject message = new JsonObject()
        .put("event", event.toString())
        .put("channel", buf.toString());
    eventBus.send(ADDRESS, message);

    Pack pack = packsByBot.get(event.getClient());
    event.getClient().sendMessage(pack.getNickName(), "xdcc send #" + pack.getPackNumber());
  }

  @Handler
  public void onMessage(PrivateMessageEvent event) {
    eventBus.send("bot.start", new JsonObject().put("nick", event.getMessage()));
  }

  @Handler
  public void onChannelMessage(ChannelMessageEvent event) {
    System.out.println(event);
  }

  @Handler
  public void onNickRejected(NickRejectedEvent event) {
    event.getClient().shutdown("bye!");
    eventBus.publish("bot", new JsonObject().put("error", "NickRejectedEvent " + event.getAttemptedNick()));
  }

}
