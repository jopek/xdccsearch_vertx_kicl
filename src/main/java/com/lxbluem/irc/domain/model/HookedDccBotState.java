package com.lxbluem.irc.domain.model;

import com.lxbluem.common.domain.Pack;
import lombok.EqualsAndHashCode;

import java.util.List;
import java.util.Set;

@EqualsAndHashCode(callSuper = true)
public class HookedDccBotState extends DefaultDccBotState {
    private final Runnable requestHook;

    public HookedDccBotState(Pack pack, Runnable requestHook) {
        super(pack);
        this.requestHook = requestHook;
    }

    @Override
    public Set<String> channelReferences(String channelName, Set<String> newRefs) {
        Set<String> references = super.channelReferences(channelName, newRefs);
        if (canRequestPack())
            requestHook.run();
        return references;
    }

    @Override
    public void joinedChannel(String channelName) {
        super.joinedChannel(channelName);
        if (canRequestPack())
            requestHook.run();
    }

    @Override
    public void channelNickList(String channelName, List<String> channelNickList) {
        super.channelNickList(channelName, channelNickList);

        if (canRequestPack())
            requestHook.run();
    }

    @Override
    public void nickRegistered() {
        super.nickRegistered();

        if (canRequestPack())
            requestHook.run();
    }

}
