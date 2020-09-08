package com.lxbluem.irc.domain.model.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@AllArgsConstructor
@Builder
@Getter
public class PrepareDccTransferCommand {
    private final String botNickName;
    private final DccCtcpQuery ctcpQuery;
    private final long localIp;
}
