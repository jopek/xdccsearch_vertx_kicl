package com.lxbluem.irc.domain.interactors.subhandlers;

import com.lxbluem.common.domain.events.BotFailedEvent;
import com.lxbluem.common.domain.ports.EventDispatcher;
import com.lxbluem.irc.domain.model.request.NoticeMessageCommand;
import com.lxbluem.irc.domain.model.request.ReasonedExitCommand;
import com.lxbluem.irc.domain.ports.incoming.ExitBot;
import com.lxbluem.irc.domain.ports.incoming.NoticeMessageHandler;
import com.lxbluem.irc.domain.ports.outgoing.BotStorage;
import com.lxbluem.irc.domain.ports.outgoing.StateStorage;

public class FailureNoticeMessageHandler implements NoticeMessageHandler.SubHandler {
    private final ExitBot exitBot;
    private final EventDispatcher eventDispatcher;

    public FailureNoticeMessageHandler(BotStorage botStorage, StateStorage stateStorage, ExitBot exitBot, EventDispatcher eventDispatcher) {
        this.exitBot = exitBot;
        this.eventDispatcher = eventDispatcher;
    }

    @Override
    public boolean handle(NoticeMessageCommand command) {
        String botNickName = command.getBotNickName();
        String remoteName = command.getRemoteName();
        String noticeMessage = command.getNoticeMessage();

        String lowerCaseNoticeMessage = noticeMessage.toLowerCase();
        if (lowerCaseNoticeMessage.contains("download connection failed")
                || (lowerCaseNoticeMessage.contains("closing connection") && !lowerCaseNoticeMessage.contains("transfer canceled by user"))
                || lowerCaseNoticeMessage.contains("connection refused")
                || lowerCaseNoticeMessage.contains("you already requested that pack")
        ) {
            BotFailedEvent failedEvent = new BotFailedEvent(botNickName, noticeMessage);
            eventDispatcher.dispatch(failedEvent);
            exitBot.handle(new ReasonedExitCommand(failedEvent.getBot(), failedEvent.getMessage()));
            return true;
        }
        return false;
    }

}
