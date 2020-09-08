package com.lxbluem.irc.domain.interactors;

import com.lxbluem.common.domain.events.BotNoticeEvent;
import com.lxbluem.common.domain.ports.EventDispatcher;
import com.lxbluem.irc.domain.model.request.NoticeMessageCommand;
import com.lxbluem.irc.domain.ports.incoming.NoticeMessageHandler;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

public class NoticeMessageHandlerImpl implements NoticeMessageHandler {
    private final EventDispatcher eventDispatcher;
    private final List<SubHandler> subHandlers = new ArrayList<>();

    public NoticeMessageHandlerImpl(EventDispatcher eventDispatcher) {
        this.eventDispatcher = eventDispatcher;
    }

    @Override
    public void handle(NoticeMessageCommand command) {
        AtomicBoolean isCommandHandled = new AtomicBoolean(false);
        for (SubHandler subHandler : subHandlers) {
            isCommandHandled.set(subHandler.handle(command));
            if (isCommandHandled.get())
                break;
        }

        if (isCommandHandled.get())
            return;

        String botNickName = command.getBotNickName();
        String remoteName = command.getRemoteName();
        String noticeMessage = command.getNoticeMessage();

        BotNoticeEvent botNoticeEvent = new BotNoticeEvent(botNickName, remoteName, noticeMessage);
        eventDispatcher.dispatch(botNoticeEvent);
    }

    @Override
    public void registerMessageHandler(SubHandler handler) {
        subHandlers.add(handler);
    }

}
