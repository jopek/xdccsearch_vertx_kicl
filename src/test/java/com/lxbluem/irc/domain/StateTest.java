package com.lxbluem.irc.domain;

import com.lxbluem.common.domain.Pack;
import com.lxbluem.irc.domain.model.State;
import org.junit.Test;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import static org.junit.Assert.*;

public class StateTest {

    @Test
    public void allConditionsMet() {
        Pack pack = getPack();

        Runnable runnable = () -> {
        };
        State state = new State(pack, runnable);
        state.channelReferences("#MainChannel", Arrays.asList("#2", "#3"));
        state.channelNickList("#MainChannel", Arrays.asList("user1", "user2", "user3"));
        state.channelReferences("#2", Arrays.asList());
        state.channelNickList("#2", Arrays.asList("user1", "user2", "user3"));
        state.channelNickList("#3", Arrays.asList("userd", "usere"));

        assertTrue(state.isPackRequested());
    }

    @Test
    public void allConditionsMet_different_caseInsensitive() {
        Pack pack = getPack();

        State state = new State(pack, () -> {
        });
        state.channelReferences("#MainChannel", Arrays.asList("#SecOndArY", "#3"));
        state.channelNickList("#MainChannel", Arrays.asList("user1", "user2", "user3"));
        state.channelReferences("#secondary", Arrays.asList("#irrelevant"));
        state.channelNickList("#secondary", Arrays.asList("dude"));
        state.channelReferences("#3", Arrays.asList("#unusedChannel"));
        state.channelNickList("#3", Arrays.asList("dude2"));

        assertTrue(state.isPackRequested());
    }

    @Test
    public void not_all_channels_joined() {
        Pack pack = getPack();

        State state = new State(pack, () -> {
        });
        state.channelReferences("#MainChannel", Arrays.asList("#2", "#3"));
        state.channelNickList("#MainChannel", Arrays.asList("user1", "user2", "user3"));

        assertFalse(state.isRequestingPackPossible());
        assertFalse(state.isPackRequested());
    }

    @Test
    public void no_joined_channels() {
        Pack pack = getPack();

        State state = new State(pack, () -> {
        });
        state.channelNickList("#MainChannel", Arrays.asList("user1", "user2", "user3"));

        assertFalse(state.isRequestingPackPossible());
    }

    @Test
    public void allConditionsMet_registry_required() {
        Pack pack = getPack();

        State state = new State(pack, () -> {
        });
        state.nickRegistryRequired();
        state.channelReferences("#MainChannel", Arrays.asList("#2", "#3"));
        state.channelNickList("#MainChannel", Arrays.asList("user1", "user2", "user3"));
        state.channelReferences("#2", Arrays.asList("#irrelevant"));
        state.channelNickList("#2", Arrays.asList());
        state.channelReferences("#3", Arrays.asList("#irrelevant"));
        state.channelNickList("#3", Arrays.asList());

        assertFalse(state.isPackRequested());

        state.nickRegistered();
        assertTrue(state.isPackRequested());
    }

    @Test
    public void allConditionsMet_withExecHook() {
        Pack pack = getPack();
        AtomicBoolean atomicBoolean = new AtomicBoolean();
        State state = new State(pack, () -> atomicBoolean.set(true));

        state.channelReferences("#MainChannel", Arrays.asList("#2", "#3"));
        state.channelNickList("#MainChannel", Arrays.asList("user1", "user2", "user3"));
        state.channelNickList("#2", Arrays.asList("userX", "userY", "userZ"));
        state.channelNickList("#3", Arrays.asList("userA", "userZ"));

        assertTrue(atomicBoolean.get());
    }

    @Test
    public void setTests() {
        Set<Integer> set1;
        Set<Integer> set2;

        set1 = new HashSet<>();
        set2 = new HashSet<>(Arrays.asList(2, 3));
        assertTrue(notConainedInFirst(set1, set2).containsAll(Arrays.asList(2, 3)));
        set1 = new HashSet<>(Arrays.asList(1));
        set2 = new HashSet<>(Arrays.asList(2, 3));
        assertTrue(notConainedInFirst(set1, set2).containsAll(Arrays.asList(2, 3)));
        set1 = new HashSet<>(Arrays.asList(1, 2));
        set2 = new HashSet<>(Arrays.asList(2, 3));
        assertTrue(notConainedInFirst(set1, set2).containsAll(Arrays.asList(3)));
        set1 = new HashSet<>(Arrays.asList(1, 2, 3));
        set2 = new HashSet<>(Arrays.asList(2, 3));
        assertTrue(notConainedInFirst(set1, set2).isEmpty());
    }

    private Set<Integer> notConainedInFirst(Set<Integer> set1, Set<Integer> set2) {
        return set2.stream().filter(v -> !set1.contains(v)).collect(Collectors.toSet());
    }

    @Test
    public void request_only_once() {
        Pack pack = getPack();
        AtomicInteger atomicInteger = new AtomicInteger();
        State state = new State(pack, atomicInteger::incrementAndGet);

        state.channelNickList("#MainChannel", Arrays.asList("user1", "user2", "user3"));
        state.channelReferences("#MainChannel", Arrays.asList("#2"));
        state.channelReferences("#MainChannel", Arrays.asList("#3"));

        state.channelNickList("#2", Arrays.asList("user1"));
        state.channelNickList("#3", Arrays.asList("user2", "user3"));

        state.nickRegistered();
        assertEquals(1, atomicInteger.get());
    }

    @Test
    public void remove_channel_joined() {
        Pack pack = getPack();
        AtomicInteger atomicInteger = new AtomicInteger();
        State state = new State(pack, atomicInteger::incrementAndGet);

        state.channelNickList("#MainChannel", Arrays.asList("user1", "user2", "user3"));
        state.channelReferences("#MainChannel", Arrays.asList("#2", "#3"));

        state.channelNickList("#2", Arrays.asList("user1", "user2", "user3"));
        state.channelReferences("#2", Arrays.asList("#2", "#3"));

        state.removeReferencedChannel("#3");

        assertEquals(1, atomicInteger.get());
    }

    @Test
    public void missingRemoteBotUser() {
        Pack pack = getPack();
        State state = new State(pack, () -> {
        });
        state.channelReferences("#MainChannel", Arrays.asList());
        state.channelNickList("#MainChannel", Arrays.asList("user2", "user3"));

        assertFalse(state.isRequestingPackPossible());
        assertFalse(state.isPackRequested());
    }

    @Test
    public void hashcodes_DefaultDccBotState() {
        Pack pack = getPack();
        State state = new State(pack, () -> {
        });
        int initialHash = state.hashCode();
        state.channelReferences("#MainChannel", Arrays.asList("#2", "#3"));
        int afterChannelRefsHash = state.hashCode();
        state.channelNickList("#MainChannel", Arrays.asList("user2", "user3"));
        int afterChannelNickListHash = state.hashCode();

        assertNotEquals(initialHash, afterChannelNickListHash);
        assertNotEquals(initialHash, afterChannelRefsHash);
    }

    @Test
    public void hashcodes_HookedDccBotState() {
        Pack pack = getPack();
        State state = new State(pack, () -> {
        });
        int initialHash = state.hashCode();
        state.channelReferences("#MainChannel", Arrays.asList("#2", "#3"));
        int afterChannelRefsHash = state.hashCode();
        state.channelNickList("#MainChannel", Arrays.asList("user2", "user3"));
        int afterChannelNickListHash = state.hashCode();

        assertNotEquals(initialHash, afterChannelNickListHash);
        assertNotEquals(initialHash, afterChannelRefsHash);
    }

    @Test(expected = RuntimeException.class)
    public void remoteNickNameIsNull() {
        Pack pack = Pack.builder()
                .networkName("nn")
                .serverHostName("192.168.99.100")
                .serverPort(6667)
                .channelName("#MainChannel")
                .build();
        assertNull(pack.getNickName());
        new State(pack, () -> {
        });
    }

    private Pack getPack() {
        return Pack.builder()
                .nickName("user1")
                .networkName("nn")
                .serverHostName("192.168.99.100")
                .serverPort(6667)
                .channelName("#MainChannel")
                .build();
    }
}