package com.lxbluem.irc.domain.ports.incoming;

import com.lxbluem.irc.domain.model.request.RegisterNickNameCommand;

public interface RegisterNickName {
    void handle(RegisterNickNameCommand command);
}
