package com.lxbluem.irc.domain;

import com.lxbluem.domain.Pack;
import org.junit.Test;

import java.util.Arrays;
import java.util.HashSet;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

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