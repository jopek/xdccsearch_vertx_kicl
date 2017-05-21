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
import org.kitteh.irc.client.library.element.Channel;
import org.kitteh.irc.client.library.event.channel.ChannelJoinEvent;
import org.kitteh.irc.client.library.event.channel.ChannelTopicEvent;
import org.kitteh.irc.client.library.event.client.NickRejectedEvent;
import org.kitteh.irc.client.library.event.helper.MessageEvent;
import org.kitteh.irc.client.library.event.user.PrivateCTCPQueryEvent;
import org.kitteh.irc.client.library.event.user.PrivateNoticeEvent;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.text.SimpleDateFormat;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static io.vertx.core.http.HttpMethod.POST;
import static java.util.stream.Collectors.toSet;

public class BotVerticle extends AbstractRouteVerticle {
  private static final String ADDRESS = "bot";
  private EventBus eventBus;

  private Map<Client, Pack> packsByBot = new HashMap<>();

  @Override
  public void start() {
    eventBus = vertx.eventBus();

    eventBus.consumer("bot.start", this::parseBotStartMessage);

    registerRouteWithHandler(getClass().getSimpleName(), POST, "/xfers", this::readPackInfo);
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
    String nick = getRandomNick();

    System.out.println(">>> " + nick);

    Client client = Client.builder()
        .serverHost(pack.getServerHostName())
        .serverPort(pack.getServerPort())
        .nick("nick_" + nick)
        .name("name_" + nick)
        .user("user_" + nick)
        .realName("realname_" + nick)
        .secure(false)
//        .listenInput(line -> System.out.println(sdf.format(new Date()) + ' ' + "[I] " + line))
//        .listenOutput(line -> System.out.println(sdf.format(new Date()) + ' ' + "[O] " + line))
//        .listenException(Throwable::printStackTrace)
        .build();

    System.out.println("client:::" + client);


    client.getEventManager().registerEventListener(this);

    client.addChannel(pack.getChannelName());

    packsByBot.putIfAbsent(client, pack);
  }

  private String getRandomNick() {
    StringBuilder stringBuilder = new StringBuilder();
    Random random = new Random();
    String source = "abcdefghijklmnopqrstuvwxyz01234567890";
    for (int i = 0; i < 2; i++) {
      stringBuilder.append(source.charAt(random.nextInt(source.length())));
    }
    return stringBuilder.toString();
  }

  @Handler
  public void onJoin(ChannelJoinEvent event) {

    System.out.println("user:::" + event.getUser());
    System.out.println("actor:::" + event.getActor());


    StringBuilder buf = new StringBuilder();
    event.getAffectedChannel().ifPresent(buf::append);
    JsonObject message = new JsonObject()
        .put("event", event.getClass().getSimpleName())
        .put("channel", buf.toString());
    eventBus.publish(ADDRESS, message);

    Pack pack = packsByBot.get(event.getClient());
    event.getAffectedChannel().ifPresent(channel -> {
      if (channel.getName().equalsIgnoreCase(pack.getChannelName()))
        event.getClient().sendMessage(pack.getNickName(), "xdcc send #" + pack.getPackNumber());
    });
  }


  @Handler
  public void onChannelTopic(ChannelTopicEvent event) {
    if (true)
      return;

    Set<String> channels = event.getClient()
        .getChannels()
        .stream()
        .map(Channel::getName)
        .collect(toSet());

    TopicExtractor topicExtractor = new TopicExtractor();
    event.getTopic()
        .getValue()
        .ifPresent(topic -> {
          if (topicExtractor.newChannelsMentioned(topic, channels)) {
            String[] channelsArray = new String[channels.size()];

            event.getClient().addChannel(channels.toArray(channelsArray));
          }
        });
  }

  @Handler
  public void onPrivateCTCPQuery(PrivateCTCPQueryEvent event) {
    String message = event.getMessage();
    Pattern pattern = Pattern.compile("DCC SEND ([\\w.-]+) (\\d+) (\\d+) (\\d+)");
    Matcher matcher = pattern.matcher(message);

    if (matcher.find()) {
      String fname = matcher.group(1);
      long ip = Long.parseLong(matcher.group(2));
      int port = Integer.parseInt(matcher.group(3));
      long size = Long.parseLong(matcher.group(4));
//      event.setReply(String.format("DCC ACCEPT %s %d %d", fname, 55555, 0));
//      new InetSocketAddress()
//      InetAddress.getByAddress()
    }

    eventBus.publish("bot", new JsonObject()
        .put("event", event.getClass().getSimpleName())
        .put("message", message));
  }

  @Handler
  public void onPrivateNotice(PrivateNoticeEvent event) {
    eventBus.publish("bot", new JsonObject()
        .put("event", event.getClass().getSimpleName())
        .put("message", event.getMessage()));
  }

  //  @Handler
  public void onMessage(MessageEvent event) {
//    eventBus.send("bot.start", new JsonObject().put("nick", event.getMessage()));
//    eventBus.publish("bot", new JsonObject().put("event", event));
    System.out.println(event);
  }

  @Handler
  public void onNickRejected(NickRejectedEvent event) {
    event.getClient().shutdown("bye!");
    eventBus.publish("bot", new JsonObject()
        .put("event", event.getClass().getSimpleName())
        .put("message", event.getAttemptedNick()));
  }


}
