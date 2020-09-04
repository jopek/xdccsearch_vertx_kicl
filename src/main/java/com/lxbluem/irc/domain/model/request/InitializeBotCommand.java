package com.lxbluem.irc.domain.model.request;

import com.lxbluem.common.domain.Pack;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@AllArgsConstructor
@Builder
@Getter
public class InitializeBotCommand {
private final Pack pack;
}
