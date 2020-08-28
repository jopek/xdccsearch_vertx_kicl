package com.lxbluem.irc.domain;

import com.lxbluem.common.domain.Pack;
import com.lxbluem.irc.domain.model.DccBotState;
import com.lxbluem.irc.domain.model.DefaultDccBotState;
import com.lxbluem.irc.domain.model.HookedDccBotState;
import org.junit.Test;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

import static org.junit.Assert.*;

public class DccBotStateTest {

    @Test
    public void allConditionsMet() {
        Pack pack = getPack();

        DccBotState dccDccBotStateGate = DccBotState.createDccBotState(pack);
        dccDccBotStateGate.joinedChannel("#1");
        HashSet<String> channelReferences = new HashSet<>(Arrays.asList("#2", "#3"));
        dccDccBotStateGate.channelReferences("#1", channelReferences);
        dccDccBotStateGate.channelNickList("#1", Arrays.asList("user1", "user2", "user3"));
        dccDccBotStateGate.joinedChannel("#2");
        dccDccBotStateGate.joinedChannel("#3");

        assertTrue(dccDccBotStateGate.canRequestPack());
    }

    @Test
    public void not_all_channels_joined() {
        Pack pack = getPack();

        DccBotState dccDccBotStateGate = DccBotState.createDccBotState(pack);
        dccDccBotStateGate.joinedChannel("#1");
        HashSet<String> channelReferences = new HashSet<>(Arrays.asList("#2", "#3"));
        dccDccBotStateGate.channelReferences("#1", channelReferences);
        dccDccBotStateGate.channelNickList("#1", Arrays.asList("user1", "user2", "user3"));
        dccDccBotStateGate.joinedChannel("#2");

        assertFalse(dccDccBotStateGate.canRequestPack());
    }

    @Test
    public void no_joined_channels() {
        Pack pack = getPack();

        DccBotState dccDccBotStateGate = DccBotState.createDccBotState(pack);
        dccDccBotStateGate.channelNickList("#1", Arrays.asList("user1", "user2", "user3"));

        assertFalse(dccDccBotStateGate.canRequestPack());
    }

    @Test
    public void allConditionsMet_registry_required() {
        Pack pack = getPack();

        DccBotState dccDccBotStateGate = DccBotState.createDccBotState(pack);
        dccDccBotStateGate.joinedChannel("#1");
        dccDccBotStateGate.nickRegistryRequired();
        HashSet<String> channelReferences = new HashSet<>(Arrays.asList("#2", "#3"));
        dccDccBotStateGate.channelReferences("#1", channelReferences);
        dccDccBotStateGate.channelNickList("#1", Arrays.asList("user1", "user2", "user3"));
        dccDccBotStateGate.joinedChannel("#2");
        dccDccBotStateGate.joinedChannel("#3");

        assertFalse(dccDccBotStateGate.canRequestPack());

        dccDccBotStateGate.nickRegistered();
        assertTrue(dccDccBotStateGate.canRequestPack());
    }

    @Test
    public void allConditionsMet_withExecHook() {
        Pack pack = getPack();
        AtomicBoolean atomicBoolean = new AtomicBoolean();
        DccBotState dccDccBotStateGate = new HookedDccBotState(pack, () -> atomicBoolean.set(true));

        dccDccBotStateGate.joinedChannel("#1");
        HashSet<String> channelReferences = new HashSet<>(Arrays.asList("#2", "#3"));
        dccDccBotStateGate.channelReferences("#1", channelReferences);
        dccDccBotStateGate.channelNickList("#1", Arrays.asList("user1", "user2", "user3"));
        dccDccBotStateGate.joinedChannel("#2");
        dccDccBotStateGate.joinedChannel("#3");

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
    public void channelJoin_hook_executed_each_channelRef_change() {
        Pack pack = getPack();
        DccBotState dccDccBotStateGate = new HookedDccBotState(pack, () -> {
        });

        Set<String> channelsAdded;

        dccDccBotStateGate.joinedChannel("#1");

        channelsAdded = dccDccBotStateGate.channelReferences("#1", new HashSet<>(Arrays.asList("#2", "#3")));
        assertEquals(2, channelsAdded.size());
        assertTrue(channelsAdded.containsAll(Arrays.asList("#2", "#3")));

        channelsAdded = dccDccBotStateGate.channelReferences("#1", new HashSet<>(Arrays.asList("#3", "#4")));
        assertEquals(1, channelsAdded.size());
        assertTrue(channelsAdded.containsAll(Arrays.asList("#4")));
    }

    @Test
    public void allConditionsMet_with_ExecHook_and_nickregistry() {
        Pack pack = getPack();
        AtomicBoolean atomicBoolean = new AtomicBoolean();
        DccBotState dccDccBotStateGate = new HookedDccBotState(pack, () -> atomicBoolean.set(true));

        dccDccBotStateGate.joinedChannel("#1");
        HashSet<String> channelReferences = new HashSet<>(Arrays.asList("#2", "#3"));
        dccDccBotStateGate.channelReferences("#1", channelReferences);
        dccDccBotStateGate.channelNickList("#1", Arrays.asList("user1", "user2", "user3"));
        dccDccBotStateGate.nickRegistryRequired();
        dccDccBotStateGate.joinedChannel("#2");
        dccDccBotStateGate.joinedChannel("#3");

        assertFalse(atomicBoolean.get());

        dccDccBotStateGate.nickRegistered();
        assertTrue(atomicBoolean.get());
    }

    @Test
    public void missingRemoteBotUser() {
        Pack pack = getPack();
        DccBotState workflow = new DefaultDccBotState(pack);
        workflow.joinedChannel("#1");
        HashSet<String> channelReferences = new HashSet<>(Arrays.asList("#2", "#3"));
        workflow.channelReferences("#1", channelReferences);
        workflow.channelNickList("#1", Arrays.asList("user2", "user3"));
        workflow.joinedChannel("#2");
        workflow.joinedChannel("#3");

        assertFalse(workflow.canRequestPack());
    }

    @Test
    public void hashcodes_DefaultDccBotState() {
        Pack pack = getPack();
        DccBotState workflow = new DefaultDccBotState(pack);
        int initialHash = workflow.hashCode();
        workflow.joinedChannel("#1");
        int afterJoinedHash = workflow.hashCode();
        HashSet<String> channelReferences = new HashSet<>(Arrays.asList("#2", "#3"));
        workflow.channelReferences("#1", channelReferences);
        int afterChannelRefsHash = workflow.hashCode();
        workflow.channelNickList("#1", Arrays.asList("user2", "user3"));
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
        DccBotState workflow = new HookedDccBotState(pack, () -> {
        });
        int initialHash = workflow.hashCode();
        workflow.joinedChannel("#1");
        int afterJoinedHash = workflow.hashCode();
        HashSet<String> channelReferences = new HashSet<>(Arrays.asList("#2", "#3"));
        workflow.channelReferences("#1", channelReferences);
        int afterChannelRefsHash = workflow.hashCode();
        workflow.channelNickList("#1", Arrays.asList("user2", "user3"));
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
        DccBotState.createDccBotState(pack);
    }

    private Pack getPack() {
        return Pack.builder()
                .nickName("user1")
                .networkName("nn")
                .serverHostName("192.168.99.100")
                .serverPort(6667)
                .channelName("#1")
                .build();
    }
}