package com.lxbluem.irc.domain.model;

import com.lxbluem.common.domain.Pack;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;

import java.util.*;
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
    private final Runnable decoratedRequestHook;

    public BotState(Pack pack, Runnable requestHook) {
        this.pack = pack;
        mainChannelName = pack.getChannelName().toLowerCase();
        remoteUser = pack.getNickName();
        decoratedRequestHook = () -> {
            requestHook.run();
            packRequested = true;
        };

        if (remoteUser == null) {
            throw new IllegalArgumentException("remote user bot cannot be null");
        }
    }

    public void joinedChannel(String channelName) {
        joinedChannels.add(channelName.toLowerCase());
    }

    public Set<String> channelReferences(String channelName, Collection<String> channelNames) {
        //IMPLICITLY JOINED - Join event does not always get fired
        joinedChannels.add(channelName.toLowerCase());

        if (channelName.equalsIgnoreCase(mainChannelName)) {
            Set<String> channels = channelNames.stream()
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
        //IMPLICITLY JOINED - get nick list only possible when joined
        joinedChannels.add(channelName.toLowerCase());

        if (channelName.equalsIgnoreCase(mainChannelName))
            remoteUserSeen = channelNickList.stream().anyMatch(nick -> nick.equalsIgnoreCase(remoteUser));

        if (canRequestPack()) {
            System.out.printf("channelNickList: %s REQUESTING\n", channelName);
            decoratedRequestHook.run();
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
        if (packRequested) return false;

        return remoteUserSeen && allChannelsJoined && !nickRegistryRequired
                || remoteUserSeen && allChannelsJoined && nickRegistered;
    }

    public boolean hasRequestedPack() {
        return packRequested;
    }

    public void nickRegistryRequired() {
        this.nickRegistryRequired = true;
    }

    public void nickRegistered() {
        this.nickRegistered = true;

        if (canRequestPack()) {
            decoratedRequestHook.run();
        }
    }

    public Pack getPack() {
        return pack;
    }

    public void removeReferencedChannel(String channelName) {
        referencedChannelNames.remove(channelName);
        if (canRequestPack()) {
            decoratedRequestHook.run();
        }
    }
}
