package com.lxbluem.irc.adapters;

import com.lxbluem.irc.domain.model.request.*;
import com.lxbluem.irc.domain.ports.incoming.*;
import com.lxbluem.irc.domain.ports.outgoing.IrcBot;
import lombok.extern.slf4j.Slf4j;
import net.engio.mbassy.listener.Handler;
import org.kitteh.irc.client.library.Client;
import org.kitteh.irc.client.library.defaults.DefaultClient;
import org.kitteh.irc.client.library.element.Channel;
import org.kitteh.irc.client.library.element.User;
import org.kitteh.irc.client.library.event.channel.ChannelTopicEvent;
import org.kitteh.irc.client.library.event.channel.ChannelUsersUpdatedEvent;
import org.kitteh.irc.client.library.event.client.ClientReceiveMotdEvent;
import org.kitteh.irc.client.library.event.client.ClientReceiveNumericEvent;
import org.kitteh.irc.client.library.event.client.NickRejectedEvent;
import org.kitteh.irc.client.library.event.user.PrivateCtcpQueryEvent;
import org.kitteh.irc.client.library.event.user.PrivateNoticeEvent;
import org.kitteh.irc.client.library.exception.KittehNagException;
import org.kitteh.irc.client.library.feature.filter.NumericFilter;

import java.text.SimpleDateFormat;
import java.util.*;
import java.util.function.Consumer;
import java.util.stream.Collectors;

@Slf4j
public class KittehIrcBot implements IrcBot {

    private final boolean isDebugging;
    private Client client;
    private String botName;
    private final ExitBot exitBot;
    private final NoticeMessageHandler noticeMessageHandler;
    private final PrepareDccTransfer prepareDccTransfer;
    private final LookForPackUser lookForPackUser;
    private final RegisterNickName registerNickName;
    private final ChangeNickName changeNickNameHandler;
    private final SkipProtectedChannel skipProtectedChannel;
    private final JoinMentionedChannels joinMentionedChannels;

    public KittehIrcBot(
            ExitBot exitBot,
            NoticeMessageHandler noticeMessageHandler,
            PrepareDccTransfer prepareDccTransfer,
            LookForPackUser lookForPackUser,
            JoinMentionedChannels joinMentionedChannels,
            RegisterNickName registerNickName,
            ChangeNickName changeNickName,
            SkipProtectedChannel skipProtectedChannel) {
        this.exitBot = exitBot;
        this.noticeMessageHandler = noticeMessageHandler;
        this.prepareDccTransfer = prepareDccTransfer;
        this.lookForPackUser = lookForPackUser;
        this.joinMentionedChannels = joinMentionedChannels;
        this.registerNickName = registerNickName;
        changeNickNameHandler = changeNickName;
        this.skipProtectedChannel = skipProtectedChannel;
        client = new DefaultClient();
        isDebugging = true;
    }

    @Override
    public void connect(BotConnectionDetails connectionDetails) {
        botName = connectionDetails.getBotNick();
        client = Client.builder()
                .server()
                .host(connectionDetails.getServerHostName())
                .port(connectionDetails.getServerPort())
                .secure(false)
                .then()
                .nick(botName)
                .name(connectionDetails.getName())
                .user(connectionDetails.getUser())
                .realName(connectionDetails.getRealName())
                .build();

        client.getEventManager().registerEventListener(this);
        Consumer<Exception> exceptionConsumer = e -> {
            if (e instanceof KittehNagException)
                return;
            ReasonedExitCommand exitCommand = new ReasonedExitCommand(botName, e.getMessage());
            exitBot.handle(exitCommand);
        };
        client.getExceptionListener().setConsumer(exceptionConsumer);

        if (isDebugging) {
            SimpleDateFormat sdf = new SimpleDateFormat("HH:mm:ss");
            client.setInputListener(line -> {
                if (line.contains(" PRIVMSG #")) return;
                if (line.contains(" 352 ")) return;
                if (line.contains(" 353 ")) return;
                if (line.contains(" 354 ")) return;
                System.out.println("           " + sdf.format(new Date()) + ' ' + "[I] " + line);
            });
            client.setOutputListener(line -> System.out.println("           " + sdf.format(new Date()) + ' ' + "[O] " + line));
        }

        client.connect();
    }

    @Override
    public void joinChannel(Collection<String> channelNames) {
        String[] channels = channelNames.toArray(new String[0]);
        joinChannel(channels);
    }

    @Override
    public void joinChannel(String... channelNames) {
        System.out.println("JOIN CHANNELS: " + String.join(", ", channelNames));
        if (channelNames.length > 0)
            client.addChannel(channelNames);
    }

    @Override
    public void registerNickname(String nick) {
        client.sendMessage("nickserv", String.format("register %s %s", "password", "email@gmail.com"));
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

    @Override
    public void terminate() {
        TimerTask delayedTermination = new TimerTask() {
            @Override
            public void run() {
                client.shutdown();
            }
        };
        new Timer().schedule(delayedTermination, 5000);
    }

    @Override
    public void cancelDcc(String remoteBotName) {
        client.sendMessage(remoteBotName, "xdcc cancel");
    }

    @Override
    public void startSearchListing(String remoteBotName, String packname) {
        client.sendMessage(remoteBotName, "xdcc search " + packname);
    }

    @Override
    public void stopSearchListing(String remoteBotName) {
        client.sendMessage(remoteBotName, "xdcc stop");
    }

    @Handler
    public void userListAvailable(ChannelUsersUpdatedEvent event) {
        Channel channel = event.getChannel();
        List<String> users = channel
                .getUsers()
                .stream()
                .map(User::getNick)
                .collect(Collectors.toList());
        String channelName = channel.getName();
        lookForPackUser.handle(new LookForPackUserCommand(botName, channelName, users));
    }

    @Handler
    public void onMotd(ClientReceiveMotdEvent event) {
        registerNickName.handle(new RegisterNickNameCommand(botName));
    }

    @Handler
    public void onChannelTopic(ChannelTopicEvent event) {
        String channelName = event.getChannel().getName();
        String topic = event
                .getNewTopic()
                .getValue()
                .orElse("");
        String botname = event.getClient().getNick();
        JoinMentionedChannelsCommand command = new JoinMentionedChannelsCommand(botname, channelName, topic);
        joinMentionedChannels.handle(command);
    }

    @NumericFilter(474)
    @Handler
    public void onBanFromChannel(ClientReceiveNumericEvent event) {
        System.out.printf("BANNED: BOT:%s EVENT:%s\n", botName, event);
        List<String> parameters = event.getParameters();
        String message = parameters.get(2);
        exitBot.handle(new ReasonedExitCommand(botName, message));
    }

    @NumericFilter(477)
    @Handler
    public void onAccountNeededJoinChannel(ClientReceiveNumericEvent event) {
        System.out.printf("CHANNEL NEEDS REGISTRATION:%s EVENT:%s\n", botName, event);
        List<String> parameters = event.getParameters();
        String botNickName = parameters.get(0);
        String attemptedChannel = parameters.get(1);
        String message = parameters.get(2);
        SkipProtectedChannelCommand command = new SkipProtectedChannelCommand(botNickName, attemptedChannel, message);
        skipProtectedChannel.handle(command);
    }

    @Handler
    public void onNickRejected(NickRejectedEvent event) {
        String serverMessages = event.getSource().getMessage();
        String attemptedNick = event.getAttemptedNick();

        ChangeNickNameCommand command = new ChangeNickNameCommand(attemptedNick, serverMessages);
        changeNickNameHandler.handle(command);
    }

    @Handler
    public void onPrivateNotice(PrivateNoticeEvent event) {
        String remoteNick = event.getActor().getNick();
        String botName = event.getClient().getNick();
        String noticeMessage = event.getMessage();

        NoticeMessageCommand command = new NoticeMessageCommand(botName, remoteNick, noticeMessage);
        noticeMessageHandler.handle(command);
    }

    @Handler
    public void onPrivateCTCPQuery(PrivateCtcpQueryEvent event) {
        DccCtcpQuery dccCtcpQuery = DccCtcpQuery.fromQueryString(event.getMessage());
        long localIp = event.getClient()
                .getUser()
                .map(User::getHost)
                .map(this::transformIpToLong)
                .orElse(0L);
        prepareDccTransfer.handle(new PrepareDccTransferCommand(botName, dccCtcpQuery, localIp));
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
