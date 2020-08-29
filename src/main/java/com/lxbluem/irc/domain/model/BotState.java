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
public class BotState {
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
    private final Runnable requestHook;

    public BotState(Pack pack, Runnable requestHook) {
        this.pack = pack;
        mainChannelName = pack.getChannelName().toLowerCase();
        remoteUser = pack.getNickName();
        this.requestHook = requestHook;

        if (remoteUser == null) {
            throw new IllegalArgumentException("remote user bot cannot be null");
        }
    }

    public void joinedChannel(String channelName) {
        joinedChannels.add(channelName.toLowerCase());
    }

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

    public void channelNickList(String channelName, List<String> channelNickList) {
        if (channelName.equalsIgnoreCase(mainChannelName))
            remoteUserSeen = channelNickList.stream().anyMatch(nick -> nick.equalsIgnoreCase(remoteUser));

        if (canRequestPack()) {
            System.out.printf("channelNickList: %s REQUESTING\n", channelName);
            requestHook.run();
        }
    }

    public boolean hasSeenRemoteUser() {
        return remoteUserSeen;
    }

    public boolean canRequestPack() {
        boolean main = joinedChannels.contains(mainChannelName);
        boolean allAdditional = joinedChannels.containsAll(referencedChannelNames);
        boolean allChannelsJoined = main && allAdditional;

        if (!channelReferencesSet) return false;

        return remoteUserSeen && allChannelsJoined && !nickRegistryRequired
                || remoteUserSeen && allChannelsJoined && nickRegistered;
    }

    public boolean hasRequestedPack() {
        return false;
    }

    public void nickRegistryRequired() {
        this.nickRegistryRequired = true;
    }

    public void nickRegistered() {
        this.nickRegistered = true;

        if (canRequestPack()) {
            System.out.print("nickRegistered REQUESTING\n");
            requestHook.run();
        }
    }

    public Pack getPack() {
        return pack;
    }

}
