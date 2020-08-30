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

        BotState botState = new BotState(pack, () -> {
        });
        botState.joinedChannel("#MainChannel");
        HashSet<String> channelReferences = new HashSet<>(Arrays.asList("#2", "#3"));
        botState.channelReferences("#MainChannel", channelReferences);
        botState.channelNickList("#MainChannel", Arrays.asList("user1", "user2", "user3"));
        botState.joinedChannel("#2");
        botState.joinedChannel("#3");

        assertTrue(botState.canRequestPack());
    }

    @Test
    public void allConditionsMet_different_caseInsensitive() {
        Pack pack = getPack();

        BotState botState = new BotState(pack, () -> {
        });
        botState.joinedChannel("#MainChannel");
        HashSet<String> channelReferences = new HashSet<>(Arrays.asList("#SecOndArY", "#3"));
        botState.channelReferences("#MainChannel", channelReferences);
        botState.channelNickList("#MainChannel", Arrays.asList("user1", "user2", "user3"));
        botState.joinedChannel("#secondary");
        botState.joinedChannel("#3");

        assertTrue(botState.canRequestPack());
    }

    @Test
    public void not_all_channels_joined() {
        Pack pack = getPack();

        BotState botState = new BotState(pack, () -> {
        });
        botState.joinedChannel("#MainChannel");
        HashSet<String> channelReferences = new HashSet<>(Arrays.asList("#2", "#3"));
        botState.channelReferences("#MainChannel", channelReferences);
        botState.channelNickList("#MainChannel", Arrays.asList("user1", "user2", "user3"));
        botState.joinedChannel("#2");

        assertFalse(botState.canRequestPack());
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
        botState.joinedChannel("#MainChannel");
        botState.nickRegistryRequired();
        HashSet<String> channelReferences = new HashSet<>(Arrays.asList("#2", "#3"));
        botState.channelReferences("#MainChannel", channelReferences);
        botState.channelNickList("#MainChannel", Arrays.asList("user1", "user2", "user3"));
        botState.joinedChannel("#2");
        botState.joinedChannel("#3");

        assertFalse(botState.hasRequestedPack());

        botState.nickRegistered();
        assertTrue(botState.hasRequestedPack());
    }

    @Test
    public void allConditionsMet_withExecHook() {
        Pack pack = getPack();
        AtomicBoolean atomicBoolean = new AtomicBoolean();
        BotState botState = new BotState(pack, () -> atomicBoolean.set(true));

        botState.joinedChannel("#MainChannel");
        HashSet<String> channelReferences = new HashSet<>(Arrays.asList("#2", "#3"));
        botState.channelReferences("#MainChannel", channelReferences);
        botState.channelNickList("#MainChannel", Arrays.asList("user1", "user2", "user3"));
        botState.joinedChannel("#2");
        botState.channelNickList("#2", Arrays.asList("userX", "userY", "userZ"));
        botState.joinedChannel("#3");
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
    public void allConditionsMet_with_ExecHook_and_nickregistry() {
        Pack pack = getPack();
        AtomicBoolean atomicBoolean = new AtomicBoolean();
        BotState botState = new BotState(pack, () -> atomicBoolean.set(true));

        botState.joinedChannel("#MainChannel");
        HashSet<String> channelReferences = new HashSet<>(Arrays.asList("#2", "#3"));
        botState.channelReferences("#MainChannel", channelReferences);
        botState.channelNickList("#MainChannel", Arrays.asList("user1", "user2", "user3"));
        botState.nickRegistryRequired();
        botState.joinedChannel("#2");
        botState.joinedChannel("#3");

        assertFalse(atomicBoolean.get());

        botState.nickRegistered();
        assertTrue(atomicBoolean.get());
    }

    @Test
    public void request_only_once() {
        Pack pack = getPack();
        AtomicInteger atomicInteger = new AtomicInteger();
        BotState botState = new BotState(pack, atomicInteger::incrementAndGet);

        botState.joinedChannel("#MainChannel");
        botState.channelNickList("#MainChannel", Arrays.asList("user1", "user2", "user3"));

        // e.g. via topic
        HashSet<String> channelReferences1 = new HashSet<>(Arrays.asList("#2"));
        botState.channelReferences("#MainChannel", channelReferences1);
        botState.joinedChannel("#2");

        // e.g. via private notice
        HashSet<String> channelReferences2 = new HashSet<>(Arrays.asList("#3"));
        botState.channelReferences("#MainChannel", channelReferences2);
        botState.joinedChannel("#3");
        botState.channelNickList("#3", Arrays.asList("user1", "user2", "user3"));

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
        botState.joinedChannel("#MainChannel");
        HashSet<String> channelReferences = new HashSet<>(Arrays.asList("#2", "#3"));
        botState.channelReferences("#MainChannel", channelReferences);
        botState.channelNickList("#MainChannel", Arrays.asList("user2", "user3"));
        botState.joinedChannel("#2");
        botState.joinedChannel("#3");

        assertFalse(botState.hasRequestedPack());
    }

    @Test
    public void hashcodes_DefaultDccBotState() {
        Pack pack = getPack();
        BotState workflow = new BotState(pack, () -> {
        });
        int initialHash = workflow.hashCode();
        workflow.joinedChannel("#MainChannel");
        int afterJoinedHash = workflow.hashCode();
        HashSet<String> channelReferences = new HashSet<>(Arrays.asList("#2", "#3"));
        workflow.channelReferences("#MainChannel", channelReferences);
        int afterChannelRefsHash = workflow.hashCode();
        workflow.channelNickList("#MainChannel", Arrays.asList("user2", "user3"));
        int afterChannelNickListHash = workflow.hashCode();
        workflow.joinedChannel("#2");
        workflow.joinedChannel("#3");
        int afterJoinedHash2 = workflow.hashCode();

        assertNotEquals(initialHash, afterJoinedHash);
        assertNotEquals(initialHash, afterChannelNickListHash);
        assertNotEquals(initialHash, afterChannelRefsHash);
        assertNotEquals(initialHash, afterJoinedHash2);
    }

    @Test
    public void hashcodes_HookedDccBotState() {
        Pack pack = getPack();
        BotState workflow = new BotState(pack, () -> {
        });
        int initialHash = workflow.hashCode();
        workflow.joinedChannel("#MainChannel");
        int afterJoinedHash = workflow.hashCode();
        HashSet<String> channelReferences = new HashSet<>(Arrays.asList("#2", "#3"));
        workflow.channelReferences("#MainChannel", channelReferences);
        int afterChannelRefsHash = workflow.hashCode();
        workflow.channelNickList("#MainChannel", Arrays.asList("user2", "user3"));
        int afterChannelNickListHash = workflow.hashCode();
        workflow.joinedChannel("#2");
        workflow.joinedChannel("#3");
        int afterJoinedHash2 = workflow.hashCode();

        assertNotEquals(initialHash, afterJoinedHash);
        assertNotEquals(initialHash, afterChannelNickListHash);
        assertNotEquals(initialHash, afterChannelRefsHash);
        assertNotEquals(initialHash, afterJoinedHash2);
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