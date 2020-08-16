package com.lxbluem.irc.usecase;

import com.lxbluem.domain.Pack;
import com.lxbluem.domain.ports.BotMessaging;
import com.lxbluem.irc.NameGenerator;
import com.lxbluem.irc.domain.DccBotState;
import com.lxbluem.irc.usecase.ports.BotPort;
import com.lxbluem.irc.usecase.ports.BotStorage;
import com.lxbluem.irc.usecase.ports.DccBotStateStorage;
import com.lxbluem.irc.usecase.requestmodel.BotConnectionDetails;
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
        BotPort botPort = botStorage.getBotByNick(botNick);
        DccBotState.Callback execution = () -> botPort.requestDccPack(pack.getNickName(), pack.getPackNumber());
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
                .timestamp(Instant.now(clock).toEpochMilli())
                .build();
        botMessaging.notify(renameMessage);
    }

    public void handleNoticeMessage(String botName, String remoteNick, String noticeMessage) {
        BotPort bot = botStorage.getBotByNick(botName);
        DccBotState botState = stateStorage.getBotStateByNick(botName);

        if (remoteNick.equalsIgnoreCase("nickserv")) {
            if (noticeMessage.toLowerCase().contains("your nickname is not registered. to register it, use")) {
                botState.nickRegistryRequired();
                bot.registerNickname(botName);
            }

            Pattern pattern = Pattern.compile("nickname .* registered", Pattern.CASE_INSENSITIVE);
            Matcher matcher = pattern.matcher(noticeMessage);

            if (matcher.find()) {
                botState.nickRegistered();
                return;
            }
            return;
        }

    }
}
