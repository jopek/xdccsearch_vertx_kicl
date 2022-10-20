package com.lxbluem.irc.domain.model.request;

public record BotConnectionDetails(
        String serverHostName,
        int serverPort,
        String botNick,
        String name,
        String user,
        String realName
) {
}
