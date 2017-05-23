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
import org.apache.commons.lang3.mutable.MutableObject;
import org.kitteh.irc.client.library.Client;
import org.kitteh.irc.client.library.element.Channel;
import org.kitteh.irc.client.library.event.channel.ChannelJoinEvent;
import org.kitteh.irc.client.library.event.channel.ChannelTopicEvent;
import org.kitteh.irc.client.library.event.client.NickRejectedEvent;
import org.kitteh.irc.client.library.event.user.PrivateCTCPQueryEvent;
import org.kitteh.irc.client.library.event.user.PrivateNoticeEvent;

import java.text.SimpleDateFormat;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static io.vertx.core.http.HttpMethod.POST;
import static java.util.stream.Collectors.toSet;

public class BotVerticle extends AbstractRouteVerticle {
    private EventBus eventBus;

    private Map<Client, Pack> packsByBot = new HashMap<>();

    @Override
    public void start() {
        eventBus = vertx.eventBus();

        eventBus.consumer("bot.start", this::startBot);
        eventBus.consumer("bot.dcc.finish", this::handleDccFinished);
        eventBus.consumer("bot.dcc.fail", this::handleDccFinished);

        registerRouteWithHandler(getClass().getSimpleName(), POST, "/xfers", this::readPackInfo);

        Pack mybotDCC = Pack.builder()
                .channelName("#download")
                .nickName("mybotDCC")
                .packNumber(3)
                .serverHostName("192.168.99.100")
                .serverPort(6667)
                .build();
        initTx(mybotDCC);
    }

    private void handleDccFinished(Message<JsonObject> message) {
        JsonObject body = message.body();
        MutableObject<Client> mutableClientObject = new MutableObject<>();

        packsByBot.forEach((ircClient, pack) -> {
            long packId = pack.getPackId();
            long mappedPackId = body.getJsonObject("pack").getLong("pid");
            if (packId == mappedPackId) {
                vertx.setTimer(5000, event -> ircClient.shutdown());
                mutableClientObject.setValue(ircClient);
            }
        });
        packsByBot.remove(mutableClientObject.getValue());
    }

    private void startBot(Message<Object> objectMessage) {
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
        Client client = Client.builder()
                .serverHost(pack.getServerHostName())
                .serverPort(pack.getServerPort())
                .nick("nick_" + nick)
                .name("name_" + nick)
                .user("user_" + nick)
                .realName("realname_" + nick)
                .secure(false)
                .listenInput(line -> System.out.println(sdf.format(new Date()) + ' ' + "[I] " + line))
                .listenOutput(line -> System.out.println(sdf.format(new Date()) + ' ' + "[O] " + line))
//                .listenException(Throwable::printStackTrace)
                .build();

        client.getEventManager().registerEventListener(this);

        client.addChannel(pack.getChannelName());

        packsByBot.putIfAbsent(client, pack);
    }

    private String getRandomNick() {
        StringBuilder stringBuilder = new StringBuilder();
        Random random = new Random();
        String source = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
        for (int i = 0; i < 2; i++) {
            stringBuilder.append(source.charAt(random.nextInt(source.length())));
        }
        return stringBuilder.toString();
    }

    @Handler
    public void onJoin(ChannelJoinEvent event) {
        StringBuilder buf = new StringBuilder();
        event.getAffectedChannel().ifPresent(buf::append);
        JsonObject message = new JsonObject()
                .put("event", event.getClass().getSimpleName())
                .put("channel", buf.toString());
        eventBus.publish("bot", message);

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

//    TopicExtractor topicExtractor = new TopicExtractor();
//    event.getTopic()
//        .getValue()
//        .ifPresent(topic -> {
//          if (topicExtractor.newChannelsMentioned(topic, channels)) {
//            String[] channelsArray = new String[channels.size()];
//
//            event.getClient().addChannel(channels.toArray(channelsArray));
//          }
//        });
    }

    @Handler
    public void onPrivateNotice(PrivateNoticeEvent event) {
        eventBus.publish("bot.notice", new JsonObject()
                .put("message", event.getMessage()));
    }

    @Handler
    public void onNickRejected(NickRejectedEvent event) {
        event.getClient().shutdown("bye!");
        eventBus.publish("bot", new JsonObject()
                .put("event", event.getClass().getSimpleName())
                .put("message", event.getAttemptedNick()));
    }

    @Handler
    public void onPrivateCTCPQuery(PrivateCTCPQueryEvent event) {
        String message = event.getMessage();
        if (!message.startsWith("DCC ")) {
            System.out.println("message is not a DCC command");
            return;
        }

        Pattern pattern = Pattern.compile("DCC (SEND|ACCEPT) ([\\w.-]+) (\\d+) (\\d+) (\\d+)( \\d+)?");
        Matcher matcher = pattern.matcher(message);

        if (!matcher.find()) {
            System.out.println("message is not a DCC SEND nor ACCEPT command");
            return;
        }

        String subType = matcher.group(1);
        String fname = matcher.group(2);
        long parsedIp = Long.parseLong(matcher.group(3));
        String ip = transformToIpString(parsedIp);
        int port = Integer.parseInt(matcher.group(4));
        long size = Long.parseLong(matcher.group(5));
        boolean passive = false;
        int token = 0;

        String tokenMatch = matcher.group(6);
        if (port == 0) {
            token = tokenMatch != null ? Integer.parseInt(tokenMatch.trim()) : 0;
            passive = true;
        }

        Pack pack = packsByBot.get(event.getClient());
        eventBus.send("bot.dcc.init", new JsonObject()
                .put("event", event.getClass().getSimpleName())
                .put("message", message)
                .put("ip", ip)
                .put("port", port)
                .put("size", size)
                .put("filename", fname)
                .put("passive", passive)
                .put("token", token)
                .put("pack", JsonObject.mapFrom(pack))
                .put("bot", event.getClient().getNick())
        );
    }

    private String transformToIpString(long ip) {
        StringJoiner joiner = new StringJoiner(".");
        for (int i = 3; i >= 0; i--) {
            joiner.add(String.valueOf((ip >> 8 * (i)) & 0xff));
        }
        return joiner.toString();
    }


}
