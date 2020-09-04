package com.lxbluem.irc.domain.interactors;

import com.lxbluem.common.domain.events.BotFailedEvent;
import com.lxbluem.common.domain.events.BotNoticeEvent;
import com.lxbluem.common.domain.events.DccQueuedEvent;
import com.lxbluem.common.domain.ports.EventDispatcher;
import com.lxbluem.irc.domain.model.request.ManualExitCommand;
import com.lxbluem.irc.domain.model.request.NoticeMessageCommand;
import com.lxbluem.irc.domain.ports.incoming.ExitBot;
import com.lxbluem.irc.domain.ports.incoming.NoticeMessageHandler;
import com.lxbluem.irc.domain.ports.outgoing.BotStateStorage;
import com.lxbluem.irc.domain.ports.outgoing.BotStorage;

import java.time.Clock;
import java.time.Instant;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.lxbluem.irc.domain.ChannelExtractor.getMentionedChannels;

public class NoticeMessageHandlerImpl implements NoticeMessageHandler {
    private final BotStorage botStorage;
    private final BotStateStorage stateStorage;
    private final EventDispatcher eventDispatcher;
    private final Clock clock;
    private final ExitBot exitBot;

    public NoticeMessageHandlerImpl(
            BotStorage botStorage,
            BotStateStorage stateStorage,
            EventDispatcher eventDispatcher,
            Clock clock,
            ExitBot exitBot
    ) {
        this.botStorage = botStorage;
        this.stateStorage = stateStorage;
        this.eventDispatcher = eventDispatcher;
        this.clock = clock;
        this.exitBot = exitBot;
    }

    @Override
    public void handle(NoticeMessageCommand command) {
        String botNickName = command.getBotNickName();
        String remoteName = command.getRemoteName();
        String noticeMessage = command.getNoticeMessage();

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
                        Set<String> mentionedChannels = getMentionedChannels(noticeMessage);
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
                        BotFailedEvent failedEvent = new BotFailedEvent(botNickName, nowEpochMillis(), noticeMessage);
                        eventDispatcher.dispatch(failedEvent);
                        exitBot.handle(new ManualExitCommand(failedEvent.getBot(), failedEvent.getMessage()));
                        return;
                    }

                    BotNoticeEvent botNoticeEvent = new BotNoticeEvent(botNickName, nowEpochMillis(), remoteName, noticeMessage);
                    eventDispatcher.dispatch(botNoticeEvent);
                }));
    }

    private long nowEpochMillis() {
        return Instant.now(clock).toEpochMilli();
    }
}
