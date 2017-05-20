package com.lxbluem.irc;

import com.lxbluem.RouteVerticle;
import com.lxbluem.model.PackInfo;
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

import static io.vertx.core.http.HttpMethod.POST;

public class Bot extends RouteVerticle {
  public static final String ADDRESS = "bot";
  private EventBus eventBus;

  @Override
  public void start() {
    eventBus = vertx.eventBus();
    eventBus.consumer("bot.start", this::parseBotStartMessage);

    registerRoute(POST, "/xfers");

    vertx.eventBus().consumer(getClass().getSimpleName(), message -> {
      String messageBody = message.body().toString();
      SerializedRequest request = Json.decodeValue(messageBody, SerializedRequest.class);

      Future<JsonObject> future = Future.future();
      future.compose(message::reply, Future.future().setHandler(e -> message.fail(500, e.cause().getMessage())));

      readPackInfo(request, future);
    });
  }

  private void readPackInfo(SerializedRequest request, Future<JsonObject> future) {
    PackInfo packInfo = Json.decodeValue(request.getBody(), PackInfo.class);
    future.complete(JsonObject.mapFrom(packInfo));
  }

  private void parseBotStartMessage(Message<Object> objectMessage) {
    JsonObject body = (JsonObject) objectMessage.body();
    String nick = body.getString("nick");

    startBot(nick);
  }

  private void startBot(String nick) {
    SimpleDateFormat sdf = new SimpleDateFormat("HH:mm:ss");
    Client client = Client.builder()
        .serverHost("192.168.99.100")
        .serverPort(6667)
        .secure(false)
        .nick(nick)
        .listenInput(line -> System.out.println(sdf.format(new Date()) + ' ' + "[I] " + line))
        .listenOutput(line -> System.out.println(sdf.format(new Date()) + ' ' + "[O] " + line))
        .listenException(Throwable::printStackTrace)
        .build();

    client.getEventManager().registerEventListener(this);

    client.addChannel("#download");
  }

  @Handler
  public void onJoin(ChannelJoinEvent event) {
    System.out.println(event);

    StringBuilder buf = new StringBuilder();
    event.getAffectedChannel().ifPresent(buf::append);
    JsonObject message = new JsonObject()
        .put("event", event.toString())
        .put("channel", buf.toString());
    eventBus.publish(ADDRESS, message);
  }

  @Handler
  public void onMessage(PrivateMessageEvent event) {
    System.out.println(event);
    eventBus.publish("bot.start", new JsonObject().put("nick", event.getMessage()));
  }

  @Handler
  public void onChannelMessage(ChannelMessageEvent event) {
    System.out.println(event);
  }

  @Handler
  public void onError(NickRejectedEvent event) {
    event.getClient().shutdown("bye!");
    eventBus.publish("bot", new JsonObject().put("error", "NickRejectedEvent " + event.getAttemptedNick()));
  }

}
