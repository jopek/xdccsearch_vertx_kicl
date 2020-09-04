package com.lxbluem.irc.domain.ports.incoming;

import com.lxbluem.irc.domain.model.request.CtcpQueryCommand;

public interface CtcpQueryHandler {
    void handle(CtcpQueryCommand command);
}
