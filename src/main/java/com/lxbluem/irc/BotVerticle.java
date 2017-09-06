package com.lxbluem.irc;

import com.lxbluem.AbstractRouteVerticle;
import com.lxbluem.filesystem.FilenameResolverVerticle;
import com.lxbluem.model.Pack;
import com.lxbluem.model.SerializedRequest;
import io.vertx.core.Future;
import io.vertx.core.json.Json;
import io.vertx.core.json.JsonObject;
import io.vertx.rxjava.core.eventbus.EventBus;
import io.vertx.rxjava.core.eventbus.Message;
import net.engio.mbassy.listener.Handler;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.mutable.MutableObject;
import org.kitteh.irc.client.library.Client;
import org.kitteh.irc.client.library.element.Channel;
import org.kitteh.irc.client.library.element.ServerMessage;
import org.kitteh.irc.client.library.event.channel.ChannelTopicEvent;
import org.kitteh.irc.client.library.event.channel.ChannelUsersUpdatedEvent;
import org.kitteh.irc.client.library.event.channel.RequestedChannelJoinCompleteEvent;
import org.kitteh.irc.client.library.event.client.ClientReceiveMOTDEvent;
import org.kitteh.irc.client.library.event.client.NickRejectedEvent;
import org.kitteh.irc.client.library.event.helper.ChannelEvent;
import org.kitteh.irc.client.library.event.user.PrivateCTCPQueryEvent;
import org.kitteh.irc.client.library.event.user.PrivateNoticeEvent;
import org.kitteh.irc.client.library.exception.KittehConnectionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import rx.Single;
import rx.subjects.PublishSubject;

import java.text.SimpleDateFormat;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static io.vertx.core.http.HttpMethod.GET;
import static io.vertx.core.http.HttpMethod.POST;
import static java.lang.String.format;
import static java.util.stream.Collectors.*;

public class BotVerticle extends AbstractRouteVerticle {
    private static final Logger LOG = LoggerFactory.getLogger(BotVerticle.class);

    private EventBus eventBus;

    private ChannelExtractor channelExtractor = new ChannelExtractor();

    private Map<Client, Pack> packsByBot = new HashMap<>();
    private Map<Client, Set<String>> requiredChannelsByBot = new HashMap<>();
    private Map<Client, Boolean> botRequestingPack = new HashMap<>();
    private Map<Client, Boolean> botHasSeenPackUser = new HashMap<>();

    private final PublishSubject<RequestedChannelJoinCompleteEvent> joinCompleteEventAsyncSubject = PublishSubject.create();
    private final PublishSubject<ChannelTopicEvent> topicEventAsyncSubject = PublishSubject.create();
    private final PublishSubject<ChannelUsersUpdatedEvent> usersUpdatedEventAsyncSubject = PublishSubject.create();

    @Override
    public void start() {
        eventBus = vertx.eventBus();

        eventBus.consumer("bot.dcc.finish", this::handleDccFinished);
        eventBus.consumer("bot.dcc.fail.socket", this::handleDccFinished);
        eventBus.consumer("bot.dcc.fail.connect", this::handleDccFinished);
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
            }
        });

        client.getEventManager().registerEventListener(this);
        client.addChannel(pack.getChannelName());

        packsByBot.put(client, pack);
        Set<String> channels = new HashSet<>();
        channels.add(pack.getChannelName().toLowerCase());
        requiredChannelsByBot.put(client, channels);
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

    @Handler
    public void onMotd(ClientReceiveMOTDEvent event) {
        LOG.info("received message of the day MOTD - registering this nick");
        Client client = event.getClient();
        vertx.setTimer(30_500, timerEvent -> {
            client.sendMessage("Nickserv", "register hotA1 email@address.com");
            joinRequiredChannels(client, requiredChannelsByBot.get(client));
        });
    }

    @Handler
    public void onJoin(RequestedChannelJoinCompleteEvent event) {
        joinCompleteEventAsyncSubject.onNext(event);

        String eventChannelName = event.getChannel().getName();
        boolean isRequiredChannelsJoined = isRequiredChannelsJoined(event);

        LOG.info("joined channel {}  {}", eventChannelName, isRequiredChannelsJoined ? "[all required joined]" : "");
    }

    private boolean isRequiredChannelsJoined(ChannelEvent event) {
        Client client = event.getClient();
        Set<String> currentChannels = client
                .getChannels()
                .stream()
                .map(Channel::getName)
                .map(String::toLowerCase)
                .collect(toSet());

        return requiredChannelsByBot.get(client).stream()
                .allMatch(currentChannels::contains);
    }

    @Handler
    public void onChannelTopic(ChannelTopicEvent event) {
        topicEventAsyncSubject.onNext(event);
        Client client = event.getClient();
        String packChannelName = packsByBot.get(client).getChannelName();
        String channelName = event.getChannel().getName();
        String topic = event
                .getTopic()
                .getValue()
                .orElse("");

        LOG.info("topic for channel {} : {}",
                event.getChannel().getName(),
                topic
        );

        if (channelName.equalsIgnoreCase(packChannelName))
            joinMentionedChannelNames(client, topic);

        LOG.debug("topic : new current required channels {}",
                requiredChannelsByBot.get(client)
        );
    }

    private void joinMentionedChannelNames(Client client, String text) {
        Set<String> mentionedChannels = channelExtractor.getMentionedChannels(text);
        Set<String> requiredChannels = requiredChannelsByBot.getOrDefault(client, new HashSet<>());
        requiredChannels.addAll(mentionedChannels);

        joinRequiredChannels(client, requiredChannels);
    }

    private void joinRequiredChannels(Client client, Set<String> requiredChannels) {
        Set<String> currentChannels = client
                .getChannels()
                .stream()
                .map(Channel::getName)
                .collect(toSet());

        requiredChannels.stream()
                .map(String::toLowerCase)
                .filter(c -> c.length() > 1)
                .filter(c -> !currentChannels.contains(c))
                .peek(channel -> LOG.info("joining {}", channel))
                .forEach(client::addChannel);
    }

    @Handler
    public void onChannelUsersUpdatedEvent(ChannelUsersUpdatedEvent event) {
        usersUpdatedEventAsyncSubject.onNext(event);
        Client client = event.getClient();
        Boolean userSeen = botHasSeenPackUser.getOrDefault(client, false);
        boolean requestingPack = botRequestingPack.getOrDefault(client, false);
        Pack pack = packsByBot.get(client);
        String nickName = pack.getNickName();
        Channel channel = event.getChannel();

        boolean requiredChannelsJoined = isRequiredChannelsJoined(event);
        LOG.info("user list for {} updated -- required joined: {} - user seen: {} - requesting: {}",
                channel.getName(),
                requiredChannelsJoined,
                userSeen,
                requestingPack
        );

        if (requiredChannelsJoined && userSeen && !requestingPack) {
            botRequestingPack.put(client, true);
            requestPackViaBot(client);
            return;
        }

        if (!channel.getName().equalsIgnoreCase(pack.getChannelName())) {
            LOG.debug("{} != {} - returning", channel.getName(), pack.getChannelName());
            return;
        }

        if (!channel.getNicknames().contains(nickName)) {
            shutdown(client, format("bot %s not in channel %s", pack.getNickName(), pack.getChannelName()));
            client.setInputListener(null);
            return;
        }

        botHasSeenPackUser.put(client, true);
        LOG.info("user list for {} updated -- required joined: {} - user seen: {} - requesting: {}",
                channel.getName(),
                requiredChannelsJoined,
                true,
                requestingPack
        );

        if (requiredChannelsJoined && !requestingPack) {
            botRequestingPack.put(client, true);
            requestPackViaBot(client);
        }
    }

    private void requestPackViaBot(Client client) {
        Pack pack = packsByBot.get(client);
        LOG.info("requesting pack #{} from {}", pack.getPackNumber(), pack.getNickName());
        client.sendMessage(pack.getNickName(), "xdcc send #" + pack.getPackNumber());
    }

    private void shutdown(Client client, String message) {
        LOG.info("bot {} exiting because: {}", client.getNick(), message);
    }

    private void shutdown(Client client) {
        client.shutdown("bye!");
        packsByBot.remove(client);
        requiredChannelsByBot.remove(client);
    }

    @Handler
    public void onPrivateNotice(PrivateNoticeEvent event) {
        Pack pack = packsByBot.get(event.getClient());

        String remoteNick = event.getActor().getNick();

        if (remoteNick.toLowerCase().startsWith("ls"))
            return;

        String packNickName = pack.getNickName();
        if (remoteNick.equalsIgnoreCase(packNickName))
            LOG.info("PrivateNotice from '{}': '{}'", remoteNick, event.getMessage());
        else
            LOG.debug("PrivateNotice from '{}' (pack nick '{}'): '{}'", remoteNick, packNickName, event.getMessage());

        eventBus.publish("bot.notice", new JsonObject()
                .put("message", event.getMessage())
                .put("pack", JsonObject.mapFrom(pack)));
    }

    @Handler
    public void onNickRejected(NickRejectedEvent event) {
        Client client = event.getClient();
        String newNick = event.getNewNick();

        String serverMessages = event
                .getOriginalMessages()
                .stream()
                .map(ServerMessage::getMessage)
                .collect(joining("; "));

        event.setNewNick(newNick);
        Pack pack = packsByBot.get(client);

        LOG.warn("nick {} rejected, retrying with {}", event.getAttemptedNick(), newNick);

        eventBus.publish("bot", new JsonObject()
                .put("message", serverMessages)
                .put("pack", JsonObject.mapFrom(pack)));
    }

    @Handler
    public void onPrivateCTCPQuery(PrivateCTCPQueryEvent event) {
        String message = event.getMessage();
        if (!message.startsWith("DCC ")) {
            LOG.debug("message received: {} - ignoring", message);
            return;
        }

        JsonObject ctcpQuery = getPrivateCtcpQueryParts(message);

        if (ctcpQuery.size() == 0) {
            LOG.debug("ctcpQuery: {} (size 0) - from message: {} - ignoring", ctcpQuery.encode(), message);
            return;
        }

        LOG.info("Receive {} filetransfer for '{}'", ctcpQuery.getString("transfer_type"), ctcpQuery.getString("filename"));
        LOG.debug("Receive PrivateCTCPQuery {}", ctcpQuery.encode());

        eventBus.rxSend(FilenameResolverVerticle.address, new JsonObject().put("filename", ctcpQuery.getString("filename")))
                .map(objectMessage -> (JsonObject) objectMessage.body())
                .subscribe(filenameAnswer -> {
                            Client ircClient = event.getClient();
                            Pack pack = packsByBot.get(ircClient);

                            JsonObject botInitMessage = new JsonObject()
                                    .put("event", event.getClass().getSimpleName())
                                    .put("message", message)
                                    .put("ip", ctcpQuery.getString("ip"))
                                    .put("port", ctcpQuery.getInteger("port"))
                                    .put("size", ctcpQuery.getLong("size"))
                                    .put("filename", filenameAnswer.getString("filename"))
                                    .put("token", ctcpQuery.getInteger("token"))
                                    .put("pack", JsonObject.mapFrom(pack))
                                    .put("bot", ircClient.getNick());

                            LOG.debug("saving {} -> {}", ctcpQuery.getString("filename"), filenameAnswer.getString("filename"));

                            Single<Message<JsonObject>> singleResponse = eventBus.rxSend(
                                    "bot.dcc.init." + ctcpQuery.getString("transfer_type"),
                                    botInitMessage
                            );

                            if ("active".equals(ctcpQuery.getString("transfer_type"))) {
                                singleResponse.subscribe().unsubscribe();
                                return;
                            }

                            singleResponse.subscribe(verticleReplyHandler ->
                                            ircClient.getUser().ifPresent(user -> {
                                                String host = user.getHost();
                                                String botReply = format("DCC SEND %s %s %d %d %d",
                                                        ctcpQuery.getString("filename"),
                                                        transformToIpLong(host),
                                                        verticleReplyHandler.body().getInteger("port"),
                                                        (long) ctcpQuery.getLong("size"),
                                                        ctcpQuery.getInteger("token")
                                                );

                                                ircClient.sendCTCPMessage(pack.getNickName(), botReply);
                                            }),

                                    throwable -> eventBus.publish("bot.fail", new JsonObject()
                                            .put("error", throwable.getMessage())
                                    )
                            );

                        },
                        throwable -> eventBus.publish("bot.fail", new JsonObject()
                                .put("error", throwable.getMessage())
                        )
                );

    }

    private JsonObject getPrivateCtcpQueryParts(String message) {
        Pattern pattern = Pattern.compile("DCC (SEND|ACCEPT) (\\S+) (\\d+) (\\d+) (\\d+)( \\d+)?");
        Matcher matcher = pattern.matcher(message);

        if (!matcher.find()) {
            LOG.debug("failed parsing {}", message);
            return new JsonObject();
        }

        int port = Integer.parseInt(matcher.group(4));

        JsonObject entries = new JsonObject()
                .put("filename", matcher.group(2))
                .put("parsed_ip", Long.parseLong(matcher.group(3)))
                .put("ip", transformLongToIpString(Long.parseLong(matcher.group(3))))
                .put("port", port)
                .put("size", Long.parseLong(matcher.group(5)))
                .put("transfer_type", "active")
                .put("token", 0);

        if (port == 0) {
            entries.put("token", matcher.group(6) != null ? Integer.parseInt(matcher.group(6).trim()) : 0);
            entries.put("transfer_type", "passive");
        }

        return entries;
    }

    private String transformLongToIpString(long ip) {
        StringJoiner joiner = new StringJoiner(".");
        for (int i = 3; i >= 0; i--) {
            joiner.add(String.valueOf(ip >> 8 * i & 0xff));
        }
        return joiner.toString();
    }

    private String transformToIpLong(String ipString) {
        String[] ipParts = ipString.trim().split("\\.");
        long ipLong = 0;
        try {
            for (int i = 0; i <= 3; i++) {
                ipLong += Long.parseLong(ipParts[i]) << 8 * (3 - i);
            }
            return String.valueOf(ipLong);
        } catch (NumberFormatException e) {
            return ipString;
        }
    }
}
