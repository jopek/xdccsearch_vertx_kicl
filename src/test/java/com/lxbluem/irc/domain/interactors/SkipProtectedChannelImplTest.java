package com.lxbluem.irc.domain.interactors;

import com.lxbluem.common.domain.Pack;
import com.lxbluem.irc.adapters.InMemoryBotStateStorage;
import com.lxbluem.irc.domain.model.BotState;
import com.lxbluem.irc.domain.model.request.SkipProtectedChannelCommand;
import com.lxbluem.irc.domain.ports.incoming.SkipProtectedChannel;
import com.lxbluem.irc.domain.ports.outgoing.BotStateStorage;
import org.junit.Before;
import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.Assert.assertEquals;

public class SkipProtectedChannelImplTest {

    private BotStateStorage stateStorage;
    private final AtomicInteger requestHookExecuted = new AtomicInteger();

    @Before
    public void setUp() {
        stateStorage = new InMemoryBotStateStorage();
        initialiseStorages();
    }

    private void initialiseStorages() {
        Pack pack = testPack();
        Runnable requestHook = () -> requestHookExecuted.addAndGet(1);
        stateStorage.save("Andy", new BotState(pack, requestHook));
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
    public void channel_requires_account_registry__account_register_nick() {
        BotState botState = stateStorage.get("Andy").get();
        String botNick = "Andy";
        String numericCommandMessage = "You need to be identified to a registered account to join this channel";
        botState.channelReferences("#download", Arrays.asList("#2", "#mg-lounge"));
        botState.channelNickList("#download", Arrays.asList("keex"));
        botState.channelReferences("#2", Collections.emptyList());

        SkipProtectedChannel skipProtectedChannel = new SkipProtectedChannelImpl(stateStorage);
        SkipProtectedChannelCommand command = new SkipProtectedChannelCommand(botNick, "#mg-lounge", numericCommandMessage);
        skipProtectedChannel.handle(command);

        assertEquals(1, requestHookExecuted.get());
    }


}