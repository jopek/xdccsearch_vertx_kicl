package com.lxbluem.irc.domain.interactors.subhandlers;

import com.lxbluem.common.domain.events.BotFailedEvent;
import com.lxbluem.common.domain.ports.EventDispatcher;
import com.lxbluem.irc.domain.model.request.ManualExitCommand;
import com.lxbluem.irc.domain.model.request.NoticeMessageCommand;
import com.lxbluem.irc.domain.ports.incoming.ExitBot;
import com.lxbluem.irc.domain.ports.incoming.NoticeMessageHandler;
import com.lxbluem.irc.domain.ports.outgoing.BotStateStorage;
import com.lxbluem.irc.domain.ports.outgoing.BotStorage;

import java.time.Clock;
import java.time.Instant;

public class FailureNoticeMessageHandler implements NoticeMessageHandler.SubHandler {
    private final ExitBot exitBot;
    private final EventDispatcher eventDispatcher;
    private final Clock clock;

    public FailureNoticeMessageHandler(BotStorage botStorage, BotStateStorage stateStorage, ExitBot exitBot, EventDispatcher eventDispatcher, Clock clock) {
        this.exitBot = exitBot;
        this.eventDispatcher = eventDispatcher;
        this.clock = clock;
    }

    @Override
    public boolean handle(NoticeMessageCommand command) {
        String botNickName = command.getBotNickName();
        String remoteName = command.getRemoteName();
        String noticeMessage = command.getNoticeMessage();

        String lowerCaseNoticeMessage = noticeMessage.toLowerCase();
        if (lowerCaseNoticeMessage.contains("download connection failed")
                || lowerCaseNoticeMessage.contains("closing connection")
                || lowerCaseNoticeMessage.contains("connection refused")
                || lowerCaseNoticeMessage.contains("you already requested that pack")
        ) {
            BotFailedEvent failedEvent = new BotFailedEvent(botNickName, nowEpochMillis(), noticeMessage);
            eventDispatcher.dispatch(failedEvent);
            exitBot.handle(new ManualExitCommand(failedEvent.getBot(), failedEvent.getMessage()));
            return true;
        }
        return false;
    }

    private long nowEpochMillis() {
        return Instant.now(clock).toEpochMilli();
    }
}
