package com.lxbluem.irc.usecase.requestmodel;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@AllArgsConstructor
@Builder
public class BotConnectionDetails {
    private final String serverHostName;
    private final int serverPort;
    private final String botNick;
    private final String name;
    private final String user;
    private final String realName;
}
