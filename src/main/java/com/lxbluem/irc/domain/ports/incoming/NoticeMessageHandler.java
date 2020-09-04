package com.lxbluem.irc.domain.ports.incoming;

import com.lxbluem.irc.domain.model.request.NoticeMessageCommand;

public interface NoticeMessageHandler {
    void handle(NoticeMessageCommand noticeMessageCommand);
}
