package com.lxbluem.irc;

import com.lxbluem.Messaging;
import com.lxbluem.model.Pack;
import io.vertx.core.json.JsonObject;
import io.vertx.rxjava.core.Vertx;
import io.vertx.rxjava.core.eventbus.Message;
import net.engio.mbassy.listener.Handler;
import org.kitteh.irc.client.library.Client;
import org.kitteh.irc.client.library.element.Channel;
import org.kitteh.irc.client.library.element.ServerMessage;
import org.kitteh.irc.client.library.element.User;
import org.kitteh.irc.client.library.event.channel.ChannelTopicEvent;
import org.kitteh.irc.client.library.event.channel.ChannelUsersUpdatedEvent;
import org.kitteh.irc.client.library.event.channel.RequestedChannelJoinCompleteEvent;
import org.kitteh.irc.client.library.event.client.ClientReceiveMotdEvent;
import org.kitteh.irc.client.library.event.client.NickRejectedEvent;
import org.kitteh.irc.client.library.event.helper.ChannelEvent;
import org.kitteh.irc.client.library.event.user.PrivateCtcpQueryEvent;
import org.kitteh.irc.client.library.event.user.PrivateNoticeEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.StringJoiner;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.lxbluem.Addresses.BOT_DCC_INIT;
import static com.lxbluem.Addresses.BOT_DCC_QUEUE;
import static com.lxbluem.Addresses.BOT_FAIL;
import static com.lxbluem.Addresses.BOT_NOTICE;
import static com.lxbluem.Addresses.BOT_UPDATE_NICK;
import static com.lxbluem.Addresses.FILENAME_RESOLVE;
import static java.lang.String.format;
import static java.util.stream.Collectors.joining;
import static java.util.stream.Collectors.toSet;

public class BotEventListener {
    private static final Logger LOG = LoggerFactory.getLogger(BotEventListener.class);

    private boolean requestingPack;
    private boolean hasSeenPackUser;
    private Set<String> requiredChannels = new HashSet<>();

    private final ChannelExtractor channelExtractor = new ChannelExtractor();
    private final Messaging messaging;

    private final Vertx vertx;
    private final Pack pack;

    BotEventListener(Vertx vertx, Pack pack) {
        this.vertx = vertx;
        this.pack = pack;
        messaging = new Messaging(vertx.eventBus());
        requiredChannels.add(pack.getChannelName().toLowerCase());
    }

    @Handler
    public void onMotd(ClientReceiveMotdEvent event) {
        LOG.info("received message of the day MOTD - registering this nick");
        registerNick(event.getClient());
    }

    private void registerNick(Client client) {
        vertx.setTimer(30_500, timerEvent -> {
            client.sendMessage("Nickserv", "register hotA1 email@address.com");
            joinRequiredChannels(client, requiredChannels);
        });
    }

    @Handler
    public void onJoin(RequestedChannelJoinCompleteEvent event) {
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

        return requiredChannels.stream()
                .allMatch(currentChannels::contains);
    }

    @Handler
    public void onChannelTopic(ChannelTopicEvent event) {
        Client client = event.getClient();
        String packChannelName = pack.getChannelName();
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
                requiredChannels
        );
    }

    private void joinMentionedChannelNames(Client client, String text) {
        Set<String> mentionedChannels = channelExtractor.getMentionedChannels(text);
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
        Client client = event.getClient();
        String nickName = pack.getNickName();
        Channel channel = event.getChannel();

        boolean requiredChannelsJoined = isRequiredChannelsJoined(event);
        LOG.info("user list for {} updated -- required joined: {} - user seen: {} - requesting: {}",
                channel.getName(),
                requiredChannelsJoined,
                hasSeenPackUser,
                requestingPack
        );

        if (requiredChannelsJoined && hasSeenPackUser && !requestingPack) {
            requestingPack = true;
            requestPackViaBot(client);
            return;
        }

        if (!channel.getName().equalsIgnoreCase(pack.getChannelName())) {
            LOG.debug("{} != {} - returning", channel.getName(), pack.getChannelName());
            return;
        }

        if (!channel.getNicknames().contains(nickName)) {
            final String message = format("bot %s not in channel %s", pack.getNickName(), pack.getChannelName());
            messaging.notify(BOT_FAIL, client.getNick(), message);
            return;
        }

        this.hasSeenPackUser = true;
        LOG.info("user list for {} updated -- required joined: {} - user seen: {} - requesting: {}",
                channel.getName(),
                requiredChannelsJoined,
                true,
                requestingPack
        );

        if (requiredChannelsJoined && !requestingPack) {
            requestingPack = true;
            requestPackViaBot(client);
        }
    }

    private void requestPackViaBot(Client client) {
        LOG.info("requesting pack #{} from {}", pack.getPackNumber(), pack.getNickName());
        client.sendMessage(pack.getNickName(), "xdcc send #" + pack.getPackNumber());
    }

    @Handler
    public void onPrivateNotice(PrivateNoticeEvent event) {
        final String remoteNick = event.getActor().getNick();
        final String botName = event.getClient().getNick();

        if (remoteNick.toLowerCase().startsWith("ls"))
            return;

        String packNickName = pack.getNickName();
        final String noticeMessage = event.getMessage();
        if (remoteNick.equalsIgnoreCase(packNickName))
            LOG.info("PrivateNotice from '{}': '{}'", remoteNick, noticeMessage);
        else
            LOG.debug("PrivateNotice from '{}' (pack nick '{}'): '{}'", remoteNick, packNickName, noticeMessage);

        final String noticeMessageLowerCase = noticeMessage.toLowerCase();
        if (noticeMessageLowerCase.contains("queue for pack") || noticeMessageLowerCase.contains("you already have that item queued")) {
            messaging.notify(BOT_DCC_QUEUE, botName, noticeMessage);
            return;
        }

        if (remoteNick.equalsIgnoreCase("nickserv")) {
            if (noticeMessageLowerCase.contains("your nickname is not registered. to register it, use")) {
                LOG.info("registering nick");
                registerNick(event.getClient());
                return;
            }

            Pattern pattern = Pattern.compile("nickname .* registered");
            Matcher matcher = pattern.matcher(noticeMessageLowerCase);

            if (matcher.find()) {
                LOG.debug("requesting pack");
                requestingPack = true;
                requestPackViaBot(event.getClient());
                return;
            }

        }

        if (noticeMessageLowerCase.contains("you already requested")
                || noticeMessageLowerCase.contains("download connection failed")
                || noticeMessageLowerCase.contains("connection refused")
        ) {
            messaging.notify(BOT_FAIL, botName, noticeMessage);
            return;
        }

        messaging.notify(BOT_NOTICE, botName, noticeMessage);
    }

    @Handler
    public void onNickRejected(NickRejectedEvent event) {
        String serverMessages = event
                .getOriginalMessages()
                .stream()
                .map(ServerMessage::getMessage)
                .collect(joining("; "));
        String attemptedNick = event.getAttemptedNick();
        String newNick = NameGenerator.getRandomNick();

        event.setNewNick(newNick);

        LOG.warn("nick {} rejected, retrying with {}", attemptedNick, newNick);
        final JsonObject extra = new JsonObject()
                .put("message", serverMessages)
                .put("renameto", newNick);
        messaging.notify(BOT_UPDATE_NICK, attemptedNick, extra);
    }

    @Handler
    public void onPrivateCTCPQuery(PrivateCtcpQueryEvent event) {
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

        LOG.debug("Receive PrivateCTCPQuery {}", ctcpQuery.encode());

        JsonObject filenameResolveMessage = new JsonObject().put("filename", ctcpQuery.getString("filename"));
        final String botName = event.getClient().getNick();

        vertx.eventBus()
                .<JsonObject>rxSend(FILENAME_RESOLVE, filenameResolveMessage)
                .map(Message::body)
                .flatMap(filenameAnswer -> {
                    final boolean isPassive = ctcpQuery.getString("transfer_type").equalsIgnoreCase("passive");
                    JsonObject botInitMessage = new JsonObject()
                            .put("event", event.getClass().getSimpleName())
                            .put("message", message)
                            .put("passive", isPassive)
                            .put("ip", ctcpQuery.getString("ip"))
                            .put("port", ctcpQuery.getInteger("port"))
                            .put("size", ctcpQuery.getLong("size"))
                            .put("filename", filenameAnswer.getString("filename"))
                            .put("token", ctcpQuery.getInteger("token"))
                            .put("bot", botName);

                    return vertx.eventBus().<JsonObject>rxSend(BOT_DCC_INIT, botInitMessage);
                })
                .map(Message::body)
                .subscribe(reply -> {
                            if (reply.isEmpty()) {
                                return;
                            }

                            Client client = event.getClient();
                            Optional<User> userOptional = client.getUser();
                            userOptional.ifPresent(user -> {
                                String host = user.getHost();
                                String botReply = format("DCC SEND %s %s %d %d %d",
                                        ctcpQuery.getString("filename"),
                                        transformIpToLong(host),
                                        reply.getInteger("port"),
                                        ctcpQuery.getLong("size"),
                                        ctcpQuery.getInteger("token")
                                );

                                client.sendCtcpMessage(pack.getNickName(), botReply);
                            });
                        },
                        throwable -> {
                            LOG.error("subscribe to verticle reply failed: {}", throwable.getMessage());
                            messaging.notify(BOT_FAIL, botName, throwable);
                        }
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

    private String transformIpToLong(String ipString) {
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
