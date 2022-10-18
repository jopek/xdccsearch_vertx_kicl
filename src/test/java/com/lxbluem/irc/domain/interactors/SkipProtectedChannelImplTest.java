package com.lxbluem.irc.domain.interactors;

import com.lxbluem.common.domain.Pack;
import com.lxbluem.irc.adapters.InMemoryStateStorage;
import com.lxbluem.irc.domain.model.State;
import com.lxbluem.irc.domain.model.request.SkipProtectedChannelCommand;
import com.lxbluem.irc.domain.ports.incoming.SkipProtectedChannel;
import com.lxbluem.irc.domain.ports.outgoing.StateStorage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;

class SkipProtectedChannelImplTest {

    private StateStorage stateStorage;
    private final AtomicInteger requestHookExecuted = new AtomicInteger();

    @BeforeEach
    void setUp() {
        stateStorage = new InMemoryStateStorage();
        initialiseStorages();
    }

    private void initialiseStorages() {
        Pack pack = testPack();
        Runnable requestHook = () -> requestHookExecuted.addAndGet(1);
        stateStorage.save("Andy", new State(pack, requestHook));
    }

    private Pack testPack() {
        return Pack.builder()
                .nickName("keex")
                .networkName("nn")
                .serverHostName("192.168.99.100")
                .serverPort(6667)
                .channelName("#download")
                .packNumber(5)
                .build();
    }

    @Test
    void channel_requires_account_registry__account_register_nick() {
        State state = stateStorage.get("Andy").get();
        String botNick = "Andy";
        String numericCommandMessage = "You need to be identified to a registered account to join this channel";
        state.channelReferences("#download", Arrays.asList("#2", "#mg-lounge"));
        state.channelNickList("#download", Arrays.asList("keex"));
        state.channelReferences("#2", Collections.emptyList());

        SkipProtectedChannel skipProtectedChannel = new SkipProtectedChannelImpl(stateStorage);
        SkipProtectedChannelCommand command = new SkipProtectedChannelCommand(botNick, "#mg-lounge", numericCommandMessage);
        skipProtectedChannel.handle(command);

        assertEquals(1, requestHookExecuted.get());
    }


}
