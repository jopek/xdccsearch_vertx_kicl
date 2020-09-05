package com.lxbluem.irc.domain.interactors.subhandlers;

import com.lxbluem.common.domain.events.DccQueuedEvent;
import com.lxbluem.common.domain.ports.EventDispatcher;
import com.lxbluem.irc.domain.model.request.NoticeMessageCommand;
import com.lxbluem.irc.domain.ports.incoming.NoticeMessageHandler;

import java.time.Clock;
import java.time.Instant;

public class QueuedNoticeMessageHandler implements NoticeMessageHandler.SubHandler {
    private final EventDispatcher eventDispatcher;
    private final Clock clock;

    public QueuedNoticeMessageHandler(EventDispatcher eventDispatcher, Clock clock) {
        this.eventDispatcher = eventDispatcher;
        this.clock = clock;
    }

    @Override
    public boolean handle(NoticeMessageCommand command) {
        String botNickName = command.getBotNickName();
        String remoteName = command.getRemoteName();
        String noticeMessage = command.getNoticeMessage();

        String lowerCaseNoticeMessage = noticeMessage.toLowerCase();
        if (lowerCaseNoticeMessage.contains("queue for pack")
                || lowerCaseNoticeMessage.contains("you already have that item queued")) {
            eventDispatcher.dispatch(new DccQueuedEvent(botNickName, nowEpochMillis(), noticeMessage));
            return true;
        }

        return false;
    }

    private long nowEpochMillis() {
        return Instant.now(clock).toEpochMilli();
    }
}
