package com.lxbluem.irc.domain;

import com.lxbluem.common.domain.events.BotFailedEvent;
import com.lxbluem.common.domain.events.BotRenamedEvent;
import com.lxbluem.common.domain.ports.BotMessaging;
import com.lxbluem.common.domain.ports.EventDispatcher;
import com.lxbluem.irc.domain.model.request.ManualExitCommand;
import com.lxbluem.irc.domain.ports.incoming.ExitBot;
import com.lxbluem.irc.domain.ports.incoming.NoticeMessageHandler;
import com.lxbluem.irc.domain.ports.outgoing.BotStateStorage;
import com.lxbluem.irc.domain.ports.outgoing.BotStorage;
import com.lxbluem.irc.domain.ports.outgoing.NameGenerator;

import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Set;

import static java.lang.String.format;

public class BotService {
    private final BotStorage botStorage;
    private final BotStateStorage stateStorage;
    private final BotMessaging botMessaging;
    private final EventDispatcher eventDispatcher;
    private final Clock clock;
    private final NameGenerator nameGenerator;
    private final ExitBot exitBot;
    private final NoticeMessageHandler noticeMessageHandler;

    public BotService(
            BotStorage botStorage,
            BotStateStorage stateStorage,
            BotMessaging botMessaging,
            EventDispatcher eventDispatcher,
            Clock clock,
            NameGenerator nameGenerator,
            ExitBot exitBot,
            NoticeMessageHandler noticeMessageHandler
    ) {
        this.botStorage = botStorage;
        this.stateStorage = stateStorage;
        this.botMessaging = botMessaging;
        this.eventDispatcher = eventDispatcher;
        this.clock = clock;
        this.nameGenerator = nameGenerator;
        this.exitBot = exitBot;
        this.noticeMessageHandler = noticeMessageHandler;
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

    public void channelRequiresAccountRegistry(String botNickName, String channelName, String message) {
        stateStorage.get(botNickName).ifPresent(botState -> {
            if (message.toLowerCase().contains("registered account to join")) {
                botState.removeReferencedChannel(channelName);
            }
        });
    }

    private void botFailed(BotFailedEvent message) {
        eventDispatcher.dispatch(message);
        exitBot.handle(new ManualExitCommand(message.getBot(), message.getMessage()));
    }

    private long nowEpochMillis() {
        return Instant.now(clock).toEpochMilli();
    }
}
