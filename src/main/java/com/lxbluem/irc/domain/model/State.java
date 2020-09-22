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
public class State {
    private final Pack pack;
    private final String mainChannelName;
    private final String remoteUser;
    private boolean remoteUserSeen;
    private boolean packRequested;
    private boolean searchRequested;
    private boolean nickRegistryRequired;
    private boolean nickRegistered;
    private final Set<String> referencedChannelNames = new HashSet<>();
    private final Set<String> joinedChannels = new HashSet<>();
    private boolean dccTransferRunning;
    private final Runnable decoratedRequestHook;
    private boolean remoteSendsCorrectPack;
    private Runnable ctcpHandshake;
    private boolean packResumable;

    public State(Pack pack, Runnable requestHook) {
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
            return channels;
        }
        return Collections.emptySet();
    }

    public void channelNickList(String channelName, List<String> channelNickList) {
        //IMPLICITLY JOINED - get nick list only possible when joined
        joinedChannels.add(channelName.toLowerCase());

        if (channelName.equalsIgnoreCase(mainChannelName))
            remoteUserSeen = channelNickList.stream().anyMatch(nick -> nick.equalsIgnoreCase(remoteUser));

        if (isRequestingPackPossible()) {
            decoratedRequestHook.run();
        }
    }

    public boolean isRequestingPackPossible() {
        boolean main = joinedChannels.contains(mainChannelName);
        boolean allAdditional = joinedChannels.containsAll(referencedChannelNames);
        boolean allChannelsJoined = main && allAdditional;

        if (packRequested) return false;

        return remoteUserSeen && allChannelsJoined && !nickRegistryRequired
                || remoteUserSeen && allChannelsJoined && nickRegistered;
    }

    public void packIsResumable() {
        this.packResumable = true;
    }

    public void nickRegistryRequired() {
        this.nickRegistryRequired = true;
    }

    public void nickRegistered() {
        this.nickRegistered = true;

        if (isRequestingPackPossible()) {
            decoratedRequestHook.run();
        }
    }

    public Pack getPack() {
        return pack;
    }

    public void removeReferencedChannel(String channelName) {
        referencedChannelNames.remove(channelName);
        if (isRequestingPackPossible()) {
            decoratedRequestHook.run();
        }
    }

    public void dccTransferStopped() {
        this.dccTransferRunning = false;
    }

    public void dccTransferRunning() {
        this.dccTransferRunning = true;
    }

    public void requestSearchListing() {
        searchRequested = true;
    }

    public void stopSearchListing() {
        searchRequested = false;
    }

    public void remoteSendsCorrectPack() {
        this.remoteSendsCorrectPack = true;
    }

    public void continueCtcpHandshake() {
        ctcpHandshake.run();
    }

    public void saveCtcpHandshake(Runnable ctcpHandshake) {
        this.ctcpHandshake = ctcpHandshake;
    }
}
