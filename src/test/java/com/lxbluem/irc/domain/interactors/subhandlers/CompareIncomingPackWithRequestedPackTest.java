package com.lxbluem.irc.domain.interactors.subhandlers;

import com.lxbluem.common.domain.Pack;
import com.lxbluem.irc.adapters.InMemoryBotStateStorage;
import com.lxbluem.irc.adapters.InMemoryBotStorage;
import com.lxbluem.irc.domain.model.BotState;
import com.lxbluem.irc.domain.model.request.NoticeMessageCommand;
import com.lxbluem.irc.domain.ports.incoming.NoticeMessageHandler;
import com.lxbluem.irc.domain.ports.outgoing.BotStateStorage;
import com.lxbluem.irc.domain.ports.outgoing.BotStorage;
import com.lxbluem.irc.domain.ports.outgoing.IrcBot;
import org.junit.Before;
import org.junit.Test;

import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.*;

public class CompareIncomingPackWithRequestedPackTest {
    private IrcBot ircBot;
    private AtomicInteger requestHookExecuted;
    private NoticeMessageHandler.SubHandler handler;
    private BotState botState;

    @Before
    public void setUp() throws Exception {
        requestHookExecuted = new AtomicInteger();
        ircBot = mock(IrcBot.class);
        BotStorage botStorage = new InMemoryBotStorage();
        BotStateStorage stateStorage = new InMemoryBotStateStorage();
        handler = new CompareIncomingPackWithRequestedPack(botStorage, stateStorage);

        initializeStorages(botStorage, stateStorage);
    }

    private void initializeStorages(BotStorage botStorage, BotStateStorage stateStorage) {
        botStorage.save("Andy", ircBot);

        Pack pack = testPack();
        Runnable requestHook = () -> requestHookExecuted.incrementAndGet();
        botState = new BotState(pack, requestHook);
        stateStorage.save("Andy", botState);
    }

    private Pack testPack() {
        return Pack.builder()
                .nickName("keex")
                .packName("test1.bin")
                .networkName("nn")
                .serverHostName("192.168.99.100")
                .serverPort(6667)
                .channelName("#download")
                .packNumber(1)
                .build();
    }

    @Test
    public void incoming_pack_matches_requested_pack() {
        String noticeMessage = "** Sending you pack #1 (\"test1.bin\"), which is <1kB. (resume supported)";
        NoticeMessageCommand command = new NoticeMessageCommand("Andy", "keex", noticeMessage);

        boolean handled = handler.handle(command);

        assertTrue(handled);
        assertTrue(botState.isRemoteSendsCorrectPack());
        verifyZeroInteractions(ircBot);
    }

    @Test
    public void incoming_pack_does_not_match_requested_pack() {
        String noticeMessage = "** Sending you pack #1 (\"some_other.bin\"), which is <1kB. (resume supported)";
        NoticeMessageCommand command = new NoticeMessageCommand("Andy", "keex", noticeMessage);

        boolean handled = handler.handle(command);

        assertTrue(handled);
        assertFalse(botState.isRemoteSendsCorrectPack());
        verify(ircBot).startSearchListing("keex", "test1.bin");
    }
}