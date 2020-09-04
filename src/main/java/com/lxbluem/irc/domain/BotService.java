package com.lxbluem.irc.domain;

import com.lxbluem.common.domain.events.BotFailedEvent;
import com.lxbluem.common.domain.events.BotNoticeEvent;
import com.lxbluem.common.domain.events.BotRenamedEvent;
import com.lxbluem.common.domain.events.DccQueuedEvent;
import com.lxbluem.common.domain.ports.BotMessaging;
import com.lxbluem.common.domain.ports.EventDispatcher;
import com.lxbluem.irc.domain.model.request.DccCtcpQuery;
import com.lxbluem.irc.domain.model.request.DccInitializeRequest;
import com.lxbluem.irc.domain.model.request.FilenameResolveRequest;
import com.lxbluem.irc.domain.model.request.ManualExitCommand;
import com.lxbluem.irc.domain.ports.incoming.ExitBot;
import com.lxbluem.irc.domain.ports.outgoing.BotStateStorage;
import com.lxbluem.irc.domain.ports.outgoing.BotStorage;
import com.lxbluem.irc.domain.ports.outgoing.NameGenerator;

import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.lxbluem.common.infrastructure.Address.DCC_INITIALIZE;
import static com.lxbluem.common.infrastructure.Address.FILENAME_RESOLVE;
import static java.lang.String.format;

public class BotService {
    private final BotStorage botStorage;
    private final BotStateStorage stateStorage;
    private final BotMessaging botMessaging;
    private final EventDispatcher eventDispatcher;
    private final Clock clock;
    private final NameGenerator nameGenerator;
    private final ExitBot exitBot;

    public BotService(
            BotStorage botStorage,
            BotStateStorage stateStorage,
            BotMessaging botMessaging,
            EventDispatcher eventDispatcher,
            Clock clock,
            NameGenerator nameGenerator,
            ExitBot exitBot
    ) {
        this.botStorage = botStorage;
        this.stateStorage = stateStorage;
        this.botMessaging = botMessaging;
        this.eventDispatcher = eventDispatcher;
        this.clock = clock;
        this.nameGenerator = nameGenerator;
        this.exitBot = exitBot;
    }

    public void exit(String botNickName, String reason) {
        exitBot.handle(new ManualExitCommand(botNickName, reason));
    }

    public void usersInChannel(String botNickName, String channelName, List<String> usersInChannel) {
        stateStorage.get(botNickName).ifPresent(botState -> {
            botState.channelNickList(channelName, usersInChannel);
            if (!botState.hasSeenRemoteUser()) {
                String remoteUser = botState.getPack().getNickName();
                final String message = format("bot %s not in channel %s", remoteUser, channelName);
                BotFailedEvent failMessage = new BotFailedEvent(botNickName, nowEpochMillis(), message);
                botFailed(failMessage);
            }
        });
    }

    public void channelTopic(String botNickName, String channelName, String topic) {
        botStorage.get(botNickName).ifPresent(bot ->
                stateStorage.get(botNickName).ifPresent(botState -> {
                    Set<String> mentionedChannels = ChannelExtractor.getMentionedChannels(topic);
                    Set<String> channelReferences = botState.channelReferences(channelName, mentionedChannels);
                    bot.joinChannel(channelReferences);
                })
        );
    }

    public void messageOfTheDay(String botNickName, List<String> motd) {
        botStorage.get(botNickName).ifPresent(bot ->
                bot.registerNickname(botNickName)
        );
    }

    public void changeNick(String botNickName, String serverMessages) {
        botStorage.get(botNickName).ifPresent(bot -> {
            String newBotNickName = nameGenerator.getNick();
            bot.changeNickname(newBotNickName);
            BotRenamedEvent renameMessage = BotRenamedEvent.builder()
                    .attemptedBotName(botNickName)
                    .renameto(newBotNickName)
                    .serverMessages(serverMessages)
                    .timestamp(nowEpochMillis())
                    .build();
            eventDispatcher.dispatch(renameMessage);
        });
    }

    public void handleNoticeMessage(String botNickName, String remoteName, String noticeMessage) {
        botStorage.get(botNickName).ifPresent(bot ->
                stateStorage.get(botNickName).ifPresent(botState -> {
                    if (remoteName.toLowerCase().startsWith("ls"))
                        return;

                    String lowerCaseNoticeMessage = noticeMessage.toLowerCase();
                    if (lowerCaseNoticeMessage.contains("queue for pack") || lowerCaseNoticeMessage.contains("you already have that item queued")) {
                        eventDispatcher.dispatch(new DccQueuedEvent(botNickName, nowEpochMillis(), noticeMessage));
                        return;
                    }

                    String channelName = botState.getPack().getChannelName();
                    if (lowerCaseNoticeMessage.contains(channelName.toLowerCase())) {
                        Set<String> mentionedChannels = ChannelExtractor.getMentionedChannels(noticeMessage);
                        Set<String> references = botState.channelReferences(channelName, mentionedChannels);
                        bot.joinChannel(references);
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
                            || lowerCaseNoticeMessage.contains("you already requested that pack")
                    ) {
                        BotFailedEvent noticeFailMessage = new BotFailedEvent(botNickName, nowEpochMillis(), noticeMessage);
                        botFailed(noticeFailMessage);
                        return;
                    }

                    BotNoticeEvent botNoticeEvent = new BotNoticeEvent(botNickName, nowEpochMillis(), remoteName, noticeMessage);
                    eventDispatcher.dispatch(botNoticeEvent);
                }));
    }


    public void handleCtcpQuery(String botNickName, DccCtcpQuery ctcpQuery, long localIp) {
        if (!ctcpQuery.isValid()) {
            return;
        }
        botStorage.get(botNickName).ifPresent(bot ->
                stateStorage.get(botNickName).ifPresent(botState -> {

                    AtomicReference<String> resolvedFilename = new AtomicReference<>("");

                    Consumer<Map<String, Object>> passiveDccSocketPortConsumer = (answer) -> {
                        int passiveDccSocketPort = (int) answer.getOrDefault("port", 0);
                        if (passiveDccSocketPort == 0)
                            return;
                        String nickName = botState.getPack().getNickName();
                        String dccSendRequest = format("DCC SEND %s %d %d %d %d",
                                resolvedFilename.get(),
                                localIp,
                                passiveDccSocketPort,
                                ctcpQuery.getSize(),
                                ctcpQuery.getToken()
                        );

                        bot.sendCtcpMessage(nickName, dccSendRequest);
                    };

                    Consumer<Map<String, Object>> filenameResolverConsumer = (filenameAnswerMap) -> {
                        String filenameAnswer = String.valueOf(filenameAnswerMap.getOrDefault("filename", ""));
                        resolvedFilename.set(filenameAnswer);
                        DccInitializeRequest query = DccInitializeRequest.from(ctcpQuery, botNickName);
                        botMessaging.ask(DCC_INITIALIZE, query, passiveDccSocketPortConsumer);
                    };

                    botMessaging.ask(FILENAME_RESOLVE, new FilenameResolveRequest(ctcpQuery.getFilename()), filenameResolverConsumer);
                }));
    }

    public void channelRequiresAccountRegistry(String botNickName, String channelName, String message) {
        stateStorage.get(botNickName).ifPresent(botState -> {
            if (message.toLowerCase().contains("registered account to join")) {
                botState.removeReferencedChannel(channelName);
            }
        });
    }

    private void botFailed(BotFailedEvent message) {
        eventDispatcher.dispatch(message);
        exit(message.getBot(), message.getMessage());
    }

    private long nowEpochMillis() {
        return Instant.now(clock).toEpochMilli();
    }
}
