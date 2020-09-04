package com.lxbluem.irc.domain.model.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.util.List;

@AllArgsConstructor
@Builder
@Getter
public class UsersInChannelCommand {
    String botNickName;
    String channelName;
    List<String> usersInChannel;
}
