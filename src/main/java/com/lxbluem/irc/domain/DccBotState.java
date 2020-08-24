package com.lxbluem.irc.domain;


import com.lxbluem.domain.Pack;

import java.util.List;
import java.util.Set;

public interface DccBotState {
    void joinedChannel(String channelName);

    void channelReferences(String channelName, Set<String> referencedChannelNames);

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

    static DccBotState createHookedDccBotState(Pack pack, Callback execution) {
        return new HookedDccBotState(pack, execution);
    }

    interface Callback {
        void execute();
    }
}
