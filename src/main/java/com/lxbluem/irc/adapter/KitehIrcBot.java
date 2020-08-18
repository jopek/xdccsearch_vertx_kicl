package com.lxbluem.irc.adapter;

import com.lxbluem.irc.usecase.BotService;
import com.lxbluem.irc.usecase.ports.BotPort;
import com.lxbluem.irc.usecase.requestmodel.BotConnectionDetails;
import com.lxbluem.irc.usecase.requestmodel.DccCtcpQuery;
import net.engio.mbassy.listener.Handler;
import org.kitteh.irc.client.library.Client;
import org.kitteh.irc.client.library.defaults.DefaultClient;
import org.kitteh.irc.client.library.element.Channel;
import org.kitteh.irc.client.library.element.ServerMessage;
import org.kitteh.irc.client.library.element.User;
import org.kitteh.irc.client.library.event.channel.ChannelTopicEvent;
import org.kitteh.irc.client.library.event.channel.ChannelUsersUpdatedEvent;
import org.kitteh.irc.client.library.event.channel.RequestedChannelJoinCompleteEvent;
import org.kitteh.irc.client.library.event.client.ClientReceiveMotdEvent;
import org.kitteh.irc.client.library.event.client.NickRejectedEvent;
import org.kitteh.irc.client.library.event.user.PrivateCtcpQueryEvent;
import org.kitteh.irc.client.library.event.user.PrivateNoticeEvent;

import java.text.SimpleDateFormat;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

import static java.util.stream.Collectors.joining;

public class KitehIrcBot implements BotPort {

    private final boolean isDebugging;
    private final BotService botService;
    private Client client;
    private String botName;

    public KitehIrcBot(BotService botService) {
        this.botService = botService;
        client = new DefaultClient();
        isDebugging = true;
    }

    @Override
    public void connect(BotConnectionDetails connectionDetails) {
        botName = connectionDetails.getBotNick();

        client = Client.builder()
                .serverHost(connectionDetails.getServerHostName())
                .serverPort(connectionDetails.getServerPort())
                .nick(botName)
                .name(connectionDetails.getName())
                .user(connectionDetails.getUser())
                .realName(connectionDetails.getRealName())
                .secure(false)
                .build();

        client.getEventManager().registerEventListener(this);

        if (isDebugging) {
            SimpleDateFormat sdf = new SimpleDateFormat("HH:mm:ss");
            client.setInputListener(line -> System.out.println("           " + sdf.format(new Date()) + ' ' + "[I] " + line));
            client.setOutputListener(line -> System.out.println("           " + sdf.format(new Date()) + ' ' + "[O] " + line));
        }

        client.connect();
    }

    @Override
    public void joinChannel(String... channelNames) {
        client.addChannel(channelNames);
    }

    @Override
    public void registerNickname(String nick) {
        client.sendMessage("nickserv", String.format("register %s %s", "password", "email"));
    }

    @Override
    public void changeNickname(String newNick) {
        client.setNick(newNick);
    }

    @Override
    public void requestDccPack(String remoteBotName, int packNumber) {
        client.sendMessage(remoteBotName, String.format("xdcc send #%d", packNumber));
    }

    @Override
    public void sendCtcpMessage(String nick, String message) {
        client.sendCtcpMessage(nick, message);
    }


    @Handler
    public void channelJoined(RequestedChannelJoinCompleteEvent event) {
        botService.onRequestedChannelJoinComplete(botName, event.getChannel().getName());
    }

    @Handler
    public void userListAvailable(ChannelUsersUpdatedEvent event) {
        Channel channel = event.getChannel();
        List<String> usersInChannel = channel
                .getUsers()
                .stream()
                .map(User::getNick)
                .collect(Collectors.toList());
        String channelName = channel.getName();
        botService.usersInChannel(botName, channelName, usersInChannel);
    }

    @Handler
    public void onMotd(ClientReceiveMotdEvent event) {
        List<String> motd = event.getMotd().orElse(Collections.emptyList());
        botService.messageOfTheDay(botName, motd);
    }

    @Handler
    public void onChannelTopic(ChannelTopicEvent event) {
        String channelName = event.getChannel().getName();
        String topic = event
                .getTopic()
                .getValue()
                .orElse("");
        String botname = event.getClient().getNick();
        botService.channelTopic(botname, channelName, topic);
    }

    @Handler
    public void onNickRejected(NickRejectedEvent event) {
        String serverMessages = event
                .getOriginalMessages()
                .stream()
                .map(ServerMessage::getMessage)
                .collect(joining("; "));
        String attemptedNick = event.getAttemptedNick();

        botService.changeNick(attemptedNick, serverMessages);
    }

    @Handler
    public void onPrivateNotice(PrivateNoticeEvent event) {
        String remoteNick = event.getActor().getNick();
        String botName = event.getClient().getNick();
        String noticeMessage = event.getMessage();

        botService.handleNoticeMessage(botName, remoteNick, noticeMessage);
    }

    @Handler
    public void onPrivateCTCPQuery(PrivateCtcpQueryEvent event) {
        DccCtcpQuery dccCtcpQuery = DccCtcpQuery.fromQueryString(event.getMessage());
        long localIp = event.getClient()
                .getUser()
                .map(User::getHost)
                .map(this::transformIpToLong)
                .orElse(0L);
        botService.handleCtcpQuery(botName, dccCtcpQuery, localIp);
    }

    private long transformIpToLong(String ipString) {
        String[] ipParts = ipString.trim().split("\\.");
        long ipLong = 0;
        try {
            for (int i = 0; i <= 3; i++) {
                ipLong += Long.parseLong(ipParts[i]) << 8 * (3 - i);
            }
            return ipLong;
        } catch (NumberFormatException e) {
            return 0;
        }
    }

}
