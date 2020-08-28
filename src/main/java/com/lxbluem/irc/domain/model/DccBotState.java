package com.lxbluem.irc.domain.model;


import com.lxbluem.common.domain.Pack;

import java.util.List;
import java.util.Set;

public interface DccBotState {
    void joinedChannel(String channelName);

    Set<String> channelReferences(String channelName, Set<String> referencedChannelNames);

    void channelNickList(String channelName, List<String> channelNickList);

    boolean hasSeenRemoteUser();

    boolean canRequestPack();

    boolean hasRequestedPack();

    void nickRegistryRequired();

    void nickRegistered();

    Pack getPack();

    static DccBotState createDccBotState(Pack pack) {
        return new DefaultDccBotState(pack);
    }

    static DccBotState createHookedDccBotState(Pack pack, Runnable requestHook) {
        return new HookedDccBotState(pack, requestHook);
    }

    interface Callback {
        void execute();
    }
}
