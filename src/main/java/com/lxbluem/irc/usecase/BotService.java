package com.lxbluem.irc.usecase;

import com.lxbluem.Address;
import com.lxbluem.domain.Pack;
import com.lxbluem.domain.ports.BotMessaging;
import com.lxbluem.irc.NameGenerator;
import com.lxbluem.irc.domain.DccBotState;
import com.lxbluem.irc.usecase.ports.BotPort;
import com.lxbluem.irc.usecase.ports.BotStorage;
import com.lxbluem.irc.usecase.ports.DccBotStateStorage;
import com.lxbluem.irc.usecase.requestmodel.*;

import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static java.lang.String.format;

public class BotService {
    private final BotStorage botStorage;
    private final DccBotStateStorage stateStorage;
    private final BotMessaging botMessaging;
    private final BotFactory botFactory;
    private final Clock clock;
    private final NameGenerator nameGenerator;

    public BotService(
            BotStorage botStorage,
            DccBotStateStorage stateStorage,
            BotMessaging botMessaging,
            BotFactory botFactory,
            Clock clock,
            NameGenerator nameGenerator) {
        this.botStorage = botStorage;
        this.stateStorage = stateStorage;
        this.botMessaging = botMessaging;
        this.botFactory = botFactory;
        this.clock = clock;
        this.nameGenerator = nameGenerator;
    }

    public String initializeBot(Pack pack) {
        String botNick = nameGenerator.getNick();

        BotPort bot = botFactory.createNewInstance(this);
        botStorage.save(botNick, bot);

        BotConnectionDetails botConnectionDetails = connectionDetailsFromPack(pack, botNick);
        bot.connect(botConnectionDetails);
        bot.joinChannel(pack.getChannelName());

        DccBotState dccBotState = DccBotState.createHookedDccBotState(pack, dccRequestHook(botNick, pack));
        stateStorage.save(botNick, dccBotState);

        botMessaging.notify(Address.BOT_INIT, new BotInitMessage(botNick, nowEpochMillis(), pack));

        return botNick;
    }

    private BotConnectionDetails connectionDetailsFromPack(Pack pack, String botNick) {
        return BotConnectionDetails.builder()
                .botNick(botNick)
                .name("name_" + botNick)
                .user("user_" + botNick)
                .realName("realname_" + botNick)
                .serverHostName(pack.getServerHostName())
                .serverPort(pack.getServerPort())
                .build();
    }

    private DccBotState.Callback dccRequestHook(String botNick, Pack pack) {
        return () -> {
            botStorage.getBotByNick(botNick)
                    .requestDccPack(pack.getNickName(), pack.getPackNumber());

            String noticeMessage = String.format("requesting pack #%s from %s", pack.getPackNumber(), pack.getNickName());
            BotNoticeMessage message = new BotNoticeMessage(botNick, nowEpochMillis(), "", noticeMessage);
            botMessaging.notify(Address.BOT_NOTICE, message);
        };
    }

        botMessaging.notify(Address.BOT_INIT, new BotInitMessage(botNick, nowEpochMillis(), pack));
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
        String randomNick = nameGenerator.getNick();
        bot.changeNickname(randomNick);
        BotRenameMessage renameMessage = BotRenameMessage.builder()
                .attemptedBotName(botName)
                .newBotName(randomNick)
                .serverMessages(serverMessages)
                .timestamp(nowEpochMillis())
                .build();
        botMessaging.notify(Address.BOT_UPDATE_NICK, renameMessage);
    }

    public void handleNoticeMessage(String botName, String remoteNick, String noticeMessage) {
        BotPort bot = botStorage.getBotByNick(botName);
        DccBotState botState = stateStorage.getBotStateByNick(botName);

        if (remoteNick.toLowerCase().startsWith("ls"))
            return;

        String lowerCaseNoticeMessage = noticeMessage.toLowerCase();
        if (lowerCaseNoticeMessage.contains("queue for pack") || lowerCaseNoticeMessage.contains("you already have that item queued")) {
            botMessaging.notify(Address.BOT_DCC_QUEUE, new BotDccQueueMessage(botName, nowEpochMillis(), noticeMessage));
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
            botMessaging.notify(Address.BOT_FAIL, new BotFailMessage(botName, nowEpochMillis(), noticeMessage));
            return;
        }

        botMessaging.notify(Address.BOT_NOTICE, new BotNoticeMessage(botName, nowEpochMillis(), remoteNick, noticeMessage));
    }

    public void handleCtcpQuery(String botNick, DccCtcpQuery ctcpQuery, long localIp) {
        if (!ctcpQuery.isValid()) {
            return;
        }

        BotPort botPort = botStorage.getBotByNick(botNick);
        DccBotState botState = stateStorage.getBotStateByNick(botNick);

        AtomicReference<String> resolvedFilename = new AtomicReference<>("");

        Consumer<Map<String, Object>> passiveDccSocketPortConsumer = (answer) -> {
            int passiveDccSocketPort = (int) answer.getOrDefault("port", 0);
            String nickName = botState.getPack().getNickName();
            String dccSendRequest = format("DCC SEND %s %d %d %d %d",
                    resolvedFilename.get(),
                    localIp,
                    passiveDccSocketPort,
                    ctcpQuery.getSize(),
                    ctcpQuery.getToken()
            );

            botPort.sendCtcpMessage(nickName, dccSendRequest);
        };

        Consumer<Map<String, Object>> filenameResolverConsumer = (filenameAnswerMap) -> {
            String filenameAnswer = String.valueOf(filenameAnswerMap.getOrDefault("filename", ""));
            resolvedFilename.set(filenameAnswer);
            botMessaging.ask(Address.BOT_DCC_INIT, ctcpQuery, passiveDccSocketPortConsumer);
        };

        botMessaging.ask(Address.FILENAME_RESOLVE, new FilenameResolveRequest(ctcpQuery.getFilename()), filenameResolverConsumer);
    }

    private long nowEpochMillis() {
        return Instant.now(clock).toEpochMilli();
    }
}
