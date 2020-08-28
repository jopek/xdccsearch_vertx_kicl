package com.lxbluem.irc.domain.model;

import com.lxbluem.common.domain.Pack;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

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
    private boolean channelReferencesSet;
    private boolean nickRegistryRequired;
    private boolean nickRegistered;
    private final Set<String> referencedChannelNames = new HashSet<>();
    private final Set<String> joinedChannels = new HashSet<>();

    public DefaultDccBotState(Pack pack) {
        this.pack = pack;
        mainChannelName = pack.getChannelName().toLowerCase();
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
    public Set<String> channelReferences(String channelName, Set<String> newRefs) {
        if (channelName.equalsIgnoreCase(mainChannelName)) {
            Set<String> channels = newRefs.stream()
                    .map(String::toLowerCase)
                    .filter(v -> !referencedChannelNames.contains(v))
                    .filter(v -> !joinedChannels.contains(v))
                    .collect(Collectors.toSet());
            referencedChannelNames.addAll(channels);
            channelReferencesSet = true;
            return channels;
        }
        return Collections.emptySet();
    }

    @Override
    public void channelNickList(String channelName, List<String> channelNickList) {
        if (!channelName.equalsIgnoreCase(mainChannelName))
            return;

        remoteUserSeen = channelNickList.stream().anyMatch(nick -> nick.equalsIgnoreCase(remoteUser));
    }

    @Override
    public boolean hasSeenRemoteUser() {
        return remoteUserSeen;
    }

    @Override
    public boolean canRequestPack() {
        boolean main = joinedChannels.contains(mainChannelName);
        boolean allAdditional = joinedChannels.containsAll(referencedChannelNames);
        boolean allChannelsJoined = main && allAdditional;

        if (!channelReferencesSet) return false;

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
