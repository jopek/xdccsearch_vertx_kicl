package com.lxbluem.irc.usecase.ports;


import com.lxbluem.domain.Pack;

import java.util.List;

public interface BotManagementPort {
    List<String> transferBots();
    String startTransferOf(Pack pack);
}
