package com.lxbluem.irc.domain.model.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@AllArgsConstructor
@Builder
@Getter
public class ExitCommand {
    String botNickName;
}
