package com.lxbluem.irc.domain;

import com.lxbluem.common.domain.Pack;
import com.lxbluem.irc.domain.model.BotState;
import org.junit.Test;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import static org.junit.Assert.*;

public class BotStateTest {

    @Test
    public void allConditionsMet() {
        Pack pack = getPack();

        Runnable runnable = () -> {
        };
        BotState botState = new BotState(pack, runnable);
        botState.channelReferences("#MainChannel", Arrays.asList("#2", "#3"));
        botState.channelNickList("#MainChannel", Arrays.asList("user1", "user2", "user3"));
        botState.channelReferences("#2", Arrays.asList());
        botState.channelNickList("#2", Arrays.asList("user1", "user2", "user3"));
        botState.channelNickList("#3", Arrays.asList("userd", "usere"));

        assertTrue(botState.hasRequestedPack());
    }

    @Test
    public void allConditionsMet_different_caseInsensitive() {
        Pack pack = getPack();

        BotState botState = new BotState(pack, () -> {
        });
        botState.channelReferences("#MainChannel", Arrays.asList("#SecOndArY", "#3"));
        botState.channelNickList("#MainChannel", Arrays.asList("user1", "user2", "user3"));
        botState.channelReferences("#secondary", Arrays.asList("#irrelevant"));
        botState.channelNickList("#secondary", Arrays.asList("dude"));
        botState.channelReferences("#3", Arrays.asList("#unusedChannel"));
        botState.channelNickList("#3", Arrays.asList("dude2"));

        assertTrue(botState.hasRequestedPack());
    }

    @Test
    public void not_all_channels_joined() {
        Pack pack = getPack();

        BotState botState = new BotState(pack, () -> {
        });
        botState.channelReferences("#MainChannel", Arrays.asList("#2", "#3"));
        botState.channelNickList("#MainChannel", Arrays.asList("user1", "user2", "user3"));

        assertFalse(botState.canRequestPack());
        assertFalse(botState.hasRequestedPack());
    }

    @Test
    public void no_joined_channels() {
        Pack pack = getPack();

        BotState botState = new BotState(pack, () -> {
        });
        botState.channelNickList("#MainChannel", Arrays.asList("user1", "user2", "user3"));

        assertFalse(botState.canRequestPack());
    }

    @Test
    public void allConditionsMet_registry_required() {
        Pack pack = getPack();

        BotState botState = new BotState(pack, () -> {
        });
        botState.nickRegistryRequired();
        botState.channelReferences("#MainChannel", Arrays.asList("#2", "#3"));
        botState.channelNickList("#MainChannel", Arrays.asList("user1", "user2", "user3"));
        botState.channelReferences("#2", Arrays.asList("#irrelevant"));
        botState.channelNickList("#2", Arrays.asList());
        botState.channelReferences("#3", Arrays.asList("#irrelevant"));
        botState.channelNickList("#3", Arrays.asList());

        assertFalse(botState.hasRequestedPack());

        botState.nickRegistered();
        assertTrue(botState.hasRequestedPack());
    }

    @Test
    public void allConditionsMet_withExecHook() {
        Pack pack = getPack();
        AtomicBoolean atomicBoolean = new AtomicBoolean();
        BotState botState = new BotState(pack, () -> atomicBoolean.set(true));

        botState.channelReferences("#MainChannel", Arrays.asList("#2", "#3"));
        botState.channelNickList("#MainChannel", Arrays.asList("user1", "user2", "user3"));
        botState.channelNickList("#2", Arrays.asList("userX", "userY", "userZ"));
        botState.channelNickList("#3", Arrays.asList("userA", "userZ"));

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
        BotState botState = new BotState(pack, atomicInteger::incrementAndGet);

        botState.channelNickList("#MainChannel", Arrays.asList("user1", "user2", "user3"));
        botState.channelReferences("#MainChannel", Arrays.asList("#2"));
        botState.channelReferences("#MainChannel", Arrays.asList("#3"));

        botState.channelNickList("#2", Arrays.asList("user1"));
        botState.channelNickList("#3", Arrays.asList("user2", "user3"));

        botState.nickRegistered();
        assertEquals(1, atomicInteger.get());
    }

    @Test
    public void remove_channel_joined() {
        Pack pack = getPack();
        AtomicInteger atomicInteger = new AtomicInteger();
        BotState botState = new BotState(pack, atomicInteger::incrementAndGet);

        botState.channelNickList("#MainChannel", Arrays.asList("user1", "user2", "user3"));
        botState.channelReferences("#MainChannel", Arrays.asList("#2", "#3"));

        botState.channelNickList("#2", Arrays.asList("user1", "user2", "user3"));
        botState.channelReferences("#2", Arrays.asList("#2", "#3"));

        botState.removeReferencedChannel("#3");

        assertEquals(1, atomicInteger.get());
    }

    @Test
    public void missingRemoteBotUser() {
        Pack pack = getPack();
        BotState botState = new BotState(pack, () -> {
        });
        botState.channelReferences("#MainChannel", Arrays.asList());
        botState.channelNickList("#MainChannel", Arrays.asList("user2", "user3"));

        assertFalse(botState.canRequestPack());
        assertFalse(botState.hasRequestedPack());
    }

    @Test
    public void hashcodes_DefaultDccBotState() {
        Pack pack = getPack();
        BotState botState = new BotState(pack, () -> {
        });
        int initialHash = botState.hashCode();
        botState.channelReferences("#MainChannel", Arrays.asList("#2", "#3"));
        int afterChannelRefsHash = botState.hashCode();
        botState.channelNickList("#MainChannel", Arrays.asList("user2", "user3"));
        int afterChannelNickListHash = botState.hashCode();

        assertNotEquals(initialHash, afterChannelNickListHash);
        assertNotEquals(initialHash, afterChannelRefsHash);
    }

    @Test
    public void hashcodes_HookedDccBotState() {
        Pack pack = getPack();
        BotState botState = new BotState(pack, () -> {
        });
        int initialHash = botState.hashCode();
        botState.channelReferences("#MainChannel", Arrays.asList("#2", "#3"));
        int afterChannelRefsHash = botState.hashCode();
        botState.channelNickList("#MainChannel", Arrays.asList("user2", "user3"));
        int afterChannelNickListHash = botState.hashCode();

        assertNotEquals(initialHash, afterChannelNickListHash);
        assertNotEquals(initialHash, afterChannelRefsHash);
    }

    @Test(expected = RuntimeException.class)
    public void remoteNickNameIsNull() {
        Pack pack = getPack();
        pack.setNickName(null);
        new BotState(pack, () -> {
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