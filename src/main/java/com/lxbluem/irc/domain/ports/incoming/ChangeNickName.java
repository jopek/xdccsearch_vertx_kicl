package com.lxbluem.irc.domain.ports.incoming;

import com.lxbluem.irc.domain.model.request.ChangeNickNameCommand;

public interface ChangeNickName {
    void handle(ChangeNickNameCommand command);
}
