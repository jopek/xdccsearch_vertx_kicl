package com.lxbluem.irc.domain;

import com.lxbluem.domain.Pack;
import lombok.*;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Getter
@AllArgsConstructor
@Builder
@EqualsAndHashCode
public class DefaultDccBotState implements DccBotState {
    private final Pack pack;
    private final String mainChannelName;
    private final String remoteUser;
    private boolean remoteUserSeen;
    private boolean packRequested;
    private boolean nickRegistryRequired;
    private boolean nickRegistered;
    private final Set<String> referencedChannelNames = new HashSet<>();
    private final Set<String> joinedChannels = new HashSet<>();

    DefaultDccBotState(Pack pack) {
        this.pack = pack;
        mainChannelName = pack.getChannelName();
        remoteUser = pack.getNickName();

        if (remoteUser == null) {
            throw new IllegalArgumentException("remote user bot cannot be null");
        }
    }

    @Override
    public void joinedChannel(String channelName) {
        joinedChannels.add(channelName.toLowerCase());
    }

    @Override
    public void channelReferences(String channelName, Set<String> referencedChannelNames) {
        if (channelName.equalsIgnoreCase(mainChannelName))
            this.referencedChannelNames.addAll(referencedChannelNames);
    }

    @Override
    public void channelNickList(String channelName, List<String> channelNickList) {
        if (!channelName.equalsIgnoreCase(mainChannelName))
            return;

        remoteUserSeen = channelNickList.stream().anyMatch(nick -> nick.equalsIgnoreCase(remoteUser));
    }

    @Override
    public boolean canRequestPack() {
        boolean main = joinedChannels.contains(mainChannelName);
        boolean allAdditional = joinedChannels.containsAll(referencedChannelNames);
        boolean allChannelsJoined = main && allAdditional;

        return remoteUserSeen && allChannelsJoined && !nickRegistryRequired
                || remoteUserSeen && allChannelsJoined && nickRegistered;
    }

    @Override
    public boolean hasRequestedPack() {
        return false;
    }

    @Override
    public void nickRegistryRequired() {
        this.nickRegistryRequired = true;
    }

    @Override
    public void nickRegistered() {
        this.nickRegistered = true;
    }

    @Override
    public Pack getPack() {
        return pack;
    }

}
