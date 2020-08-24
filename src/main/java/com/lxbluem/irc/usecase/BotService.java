package com.lxbluem.irc.usecase;

import com.lxbluem.Address;
import com.lxbluem.domain.Pack;
import com.lxbluem.domain.ports.BotMessaging;
import com.lxbluem.irc.NameGenerator;
import com.lxbluem.irc.domain.DccBotState;
import com.lxbluem.irc.usecase.exception.BotNotFoundException;
import com.lxbluem.irc.usecase.ports.BotPort;
import com.lxbluem.irc.usecase.ports.BotStorage;
import com.lxbluem.irc.usecase.ports.DccBotStateStorage;
import com.lxbluem.irc.usecase.requestmodel.*;

import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
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
        String botNickName = nameGenerator.getNick();

        BotPort bot = botFactory.createNewInstance(this);
        botStorage.save(botNickName, bot);

        BotConnectionDetails botConnectionDetails = connectionDetailsFromPack(pack, botNickName);
        bot.connect(botConnectionDetails);
        bot.joinChannel(pack.getChannelName());

        DccBotState.Callback execution = dccRequestHook(botNickName, pack);
        DccBotState dccBotState = DccBotState.createHookedDccBotState(pack, execution);
        stateStorage.save(botNickName, dccBotState);

        botMessaging.notify(Address.BOT_INIT, new BotInitMessage(botNickName, nowEpochMillis(), pack));

        return botNickName;
    }

    private BotConnectionDetails connectionDetailsFromPack(Pack pack, String botNickName) {
        return BotConnectionDetails.builder()
                .botNick(botNickName)
                .name("name_" + botNickName)
                .user("user_" + botNickName)
                .realName("realname_" + botNickName)
                .serverHostName(pack.getServerHostName())
                .serverPort(pack.getServerPort())
                .build();
    }

    private DccBotState.Callback dccRequestHook(String botNickName, Pack pack) {
        return () -> {
            botStorage.getBotByNick(botNickName)
                    .ifPresent(bot -> {
                        bot.requestDccPack(pack.getNickName(), pack.getPackNumber());

                        String noticeMessage = String.format("requesting pack #%s from %s", pack.getPackNumber(), pack.getNickName());
                        BotNoticeMessage message = new BotNoticeMessage(botNickName, nowEpochMillis(), "", noticeMessage);
                        botMessaging.notify(Address.BOT_NOTICE, message);
                    });
        };
    }

    public void manualExit(String botNickName) {
        exit(botNickName, "requested shutdown");
    }

    public void exit(String botNickName, String reason) {
        BotPort bot = botStorage.getBotByNick(botNickName)
                .orElseThrow(() -> new BotNotFoundException(botNickName));
        bot.terminate();
        botStorage.removeBot(botNickName);
        stateStorage.removeBotState(botNickName);

        String reasonMessage = String.format("%s exiting because %s", botNickName, reason);
        BotExitMessage requested_shutdown = new BotExitMessage(botNickName, nowEpochMillis(), reasonMessage);
        botMessaging.notify(Address.BOT_EXIT, requested_shutdown);
    }

    public void onRequestedChannelJoinComplete(String botNickName, String channelName) {
        stateStorage.getBotStateByNick(botNickName)
                .ifPresent(botState ->
                        botState.joinedChannel(channelName)
                );
    }

    public void usersInChannel(String botNickName, String channelName, List<String> usersInChannel) {
        stateStorage.getBotStateByNick(botNickName).ifPresent(botState -> {
            botState.channelNickList(channelName, usersInChannel);
            if (!botState.hasSeenRemoteUser()) {
                String remoteUser = botState.getPack().getNickName();
                final String message = format("bot %s not in channel %s", remoteUser, channelName);
                botMessaging.notify(Address.BOT_FAIL, new BotFailMessage(botNickName, nowEpochMillis(), message));
            }
        });
    }

    public void channelTopic(String botNickName, String channelName, String topic) {
        stateStorage.getBotStateByNick(botNickName).ifPresent(botState -> {
            Set<String> mentionedChannels = ChannelExtractor.getMentionedChannels(topic);

            botState.channelReferences(channelName, mentionedChannels);
        });
    }

    public void messageOfTheDay(String botNickName, List<String> motd) {
        BotPort bot = botStorage.getBotByNick(botNickName)
                .orElseThrow(() -> new BotNotFoundException(botNickName));
        bot.registerNickname(botNickName);
    }

    public void changeNick(String botNickName, String serverMessages) {
        BotPort bot = botStorage.getBotByNick(botNickName)
                .orElseThrow(() -> new BotNotFoundException(botNickName));
        String newBotNickName = nameGenerator.getNick();
        bot.changeNickname(newBotNickName);
        BotRenameMessage renameMessage = BotRenameMessage.builder()
                .attemptedBotName(botNickName)
                .renameto(newBotNickName)
                .serverMessages(serverMessages)
                .timestamp(nowEpochMillis())
                .build();
        botMessaging.notify(Address.BOT_UPDATE_NICK, renameMessage);
    }

    public void handleNoticeMessage(String botNickName, String remoteName, String noticeMessage) {
        BotPort bot = botStorage.getBotByNick(botNickName)
                .orElseThrow(() -> new BotNotFoundException(botNickName));
        Optional<DccBotState> optionalDccBotState = stateStorage.getBotStateByNick(botNickName);
        DccBotState botState = optionalDccBotState.get();
        if (!optionalDccBotState.isPresent())
            return;

        if (remoteName.toLowerCase().startsWith("ls"))
            return;

        String lowerCaseNoticeMessage = noticeMessage.toLowerCase();
        if (lowerCaseNoticeMessage.contains("queue for pack") || lowerCaseNoticeMessage.contains("you already have that item queued")) {
            botMessaging.notify(Address.BOT_DCC_QUEUE, new BotDccQueueMessage(botNickName, nowEpochMillis(), noticeMessage));
            return;
        }

        if (remoteName.equalsIgnoreCase("nickserv")) {
            if (lowerCaseNoticeMessage.contains("your nickname is not registered. to register it, use")) {
                botState.nickRegistryRequired();
                bot.registerNickname(botNickName);
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
            BotFailMessage message = new BotFailMessage(botNickName, nowEpochMillis(), noticeMessage);
            botMessaging.notify(Address.BOT_FAIL, message);
            return;
        }

        BotNoticeMessage message = new BotNoticeMessage(botNickName, nowEpochMillis(), remoteName, noticeMessage);
        botMessaging.notify(Address.BOT_NOTICE, message);
    }

    public void handleCtcpQuery(String botNickName, DccCtcpQuery ctcpQuery, long localIp) {
        if (!ctcpQuery.isValid()) {
            return;
        }

        BotPort botPort = botStorage.getBotByNick(botNickName)
                .orElseThrow(() -> new BotNotFoundException(botNickName));
        Optional<DccBotState> optionalDccBotState = stateStorage.getBotStateByNick(botNickName);
        DccBotState botState = optionalDccBotState.get();
        if (!optionalDccBotState.isPresent())
            return;

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
