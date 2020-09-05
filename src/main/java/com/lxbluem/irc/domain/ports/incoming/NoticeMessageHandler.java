package com.lxbluem.irc.domain.ports.incoming;

import com.lxbluem.irc.domain.model.request.NoticeMessageCommand;

public interface NoticeMessageHandler {
    void handle(NoticeMessageCommand noticeMessageCommand);
    void registerMessageHandler(SubHandler subHandler);
    interface SubHandler {
        boolean handle(NoticeMessageCommand command);
    }
}
