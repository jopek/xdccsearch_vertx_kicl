package com.lxbluem.irc.usecase;

import com.lxbluem.domain.Pack;
import com.lxbluem.domain.ports.BotMessaging;
import com.lxbluem.irc.NameGenerator;
import com.lxbluem.irc.domain.DccBotState;
import com.lxbluem.irc.usecase.ports.BotPort;
import com.lxbluem.irc.usecase.ports.BotStorage;
import com.lxbluem.irc.usecase.ports.DccBotStateStorage;
import com.lxbluem.irc.usecase.requestmodel.BotDccQueueMessage;
import com.lxbluem.irc.usecase.requestmodel.BotFailMessage;
import com.lxbluem.irc.usecase.requestmodel.BotNoticeMessage;
import com.lxbluem.irc.usecase.requestmodel.BotRenameMessage;

import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class BotService {
    private final BotStorage botStorage;
    private final DccBotStateStorage stateStorage;
    private final BotMessaging botMessaging;
    private final Clock clock;

    public BotService(BotStorage botStorage, DccBotStateStorage stateStorage, BotMessaging botMessaging, Clock clock) {
        this.botStorage = botStorage;
        this.stateStorage = stateStorage;
        this.botMessaging = botMessaging;
        this.clock = clock;
    }

    public void init(String botNick, Pack pack) {
        DccBotState.Callback execution = () -> {
            botStorage.getBotByNick(botNick)
                    .requestDccPack(pack.getNickName(), pack.getPackNumber());

            String noticeMessage = String.format("requesting pack #%s from %s", pack.getPackNumber(), pack.getNickName());
            BotNoticeMessage message = new BotNoticeMessage(botNick, nowEpochMillis(), noticeMessage);
            botMessaging.notify(message);
        };
        DccBotState dccBotState = DccBotState.createHookedDccBotState(pack, execution);
        stateStorage.save(botNick, dccBotState);
    }

    public void onRequestedChannelJoinComplete(String botNick, String channelName) {
        DccBotState botState = stateStorage.getBotStateByNick(botNick);
        botState.joinedChannel(channelName);
    }

    public void usersInChannel(String botNick, String channelName, List<String> usersInChannel) {
        DccBotState botState = stateStorage.getBotStateByNick(botNick);
        botState.channelNickList(channelName, usersInChannel);
    }

    public void channelTopic(String botName, String channelName, String topic) {
        DccBotState botState = stateStorage.getBotStateByNick(botName);
        Set<String> mentionedChannels = ChannelExtractor.getMentionedChannels(topic);

        botState.channelReferences(channelName, mentionedChannels);
    }

    public void messageOfTheDay(String botName, List<String> motd) {
        BotPort bot = botStorage.getBotByNick(botName);
        bot.registerNickname(botName);
    }


    public void changeNick(String botName, String serverMessages) {
        BotPort bot = botStorage.getBotByNick(botName);
        String randomNick = NameGenerator.getRandomNick();
        bot.changeNickname(randomNick);
        BotRenameMessage renameMessage = BotRenameMessage.builder()
                .attemptedBotName(botName)
                .newBotName(randomNick)
                .serverMessages(serverMessages)
                .timestamp(nowEpochMillis())
                .build();
        botMessaging.notify(renameMessage);
    }

    public void handleNoticeMessage(String botName, String remoteNick, String noticeMessage) {
        BotPort bot = botStorage.getBotByNick(botName);
        DccBotState botState = stateStorage.getBotStateByNick(botName);

        if (remoteNick.toLowerCase().startsWith("ls"))
            return;

        String lowerCaseNoticeMessage = noticeMessage.toLowerCase();
        if (lowerCaseNoticeMessage.contains("queue for pack") || lowerCaseNoticeMessage.contains("you already have that item queued")) {
            botMessaging.notify(new BotDccQueueMessage(botName, nowEpochMillis(), noticeMessage));
            return;
        }

        if (remoteNick.equalsIgnoreCase("nickserv")) {
            if (lowerCaseNoticeMessage.contains("your nickname is not registered. to register it, use")) {
                botState.nickRegistryRequired();
                bot.registerNickname(botName);
                return;
            }

            Pattern pattern = Pattern.compile("nickname .* registered", Pattern.CASE_INSENSITIVE);
            Matcher matcher = pattern.matcher(noticeMessage);

            if (matcher.find()) {
                botState.nickRegistered();
                return;
            }
            return;
        }

        if (lowerCaseNoticeMessage.contains("download connection failed")
                || lowerCaseNoticeMessage.contains("connection refused")
        ) {
            botMessaging.notify(new BotFailMessage(botName, nowEpochMillis(), noticeMessage));
            return;
        }

        botMessaging.notify(new BotNoticeMessage(botName, nowEpochMillis(), noticeMessage));
    }

    private long nowEpochMillis() {
        return Instant.now(clock).toEpochMilli();
    }
}
