package com.lxbluem.irc.domain.model.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@AllArgsConstructor
@Builder
@Getter
public class DccSendTransferCommand {
    private final String botNickName;
    private final CtcpDccSend ctcpQuery;
    private final long localIp;
}
