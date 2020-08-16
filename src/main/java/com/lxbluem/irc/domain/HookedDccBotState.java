package com.lxbluem.irc.domain;

import com.lxbluem.domain.Pack;
import lombok.Getter;

import java.util.List;
import java.util.Set;

public class HookedDccBotState extends DefaultDccBotState {
    private final Callback canRequestHook;

    public HookedDccBotState(Pack pack, Callback canRequestHook) {
        super(pack);
        this.canRequestHook = canRequestHook;
    }

    @Override
    public void joinedChannel(String channelName) {
        super.joinedChannel(channelName);

        if (canRequestPack())
            canRequestHook.execute();
    }

    @Override
    public void channelNickList(String channelName, List<String> channelNickList) {
        super.channelNickList(channelName, channelNickList);

        if (canRequestPack())
            canRequestHook.execute();
    }

    @Override
    public void nickRegistered() {
        super.nickRegistered();

        if (canRequestPack())
            canRequestHook.execute();
    }

}
