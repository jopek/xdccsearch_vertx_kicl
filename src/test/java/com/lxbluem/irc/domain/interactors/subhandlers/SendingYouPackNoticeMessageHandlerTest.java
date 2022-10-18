package com.lxbluem.irc.domain.interactors.subhandlers;

import com.lxbluem.common.domain.Pack;
import com.lxbluem.irc.adapters.InMemoryBotStorage;
import com.lxbluem.irc.adapters.InMemoryStateStorage;
import com.lxbluem.irc.domain.model.State;
import com.lxbluem.irc.domain.model.request.NoticeMessageCommand;
import com.lxbluem.irc.domain.ports.incoming.NoticeMessageHandler;
import com.lxbluem.irc.domain.ports.outgoing.BotStorage;
import com.lxbluem.irc.domain.ports.outgoing.IrcBot;
import com.lxbluem.irc.domain.ports.outgoing.StateStorage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;


@ExtendWith(MockitoExtension.class)
class SendingYouPackNoticeMessageHandlerTest {
    @Mock
    private IrcBot ircBot;
    private AtomicInteger requestHookExecuted;
    private NoticeMessageHandler.SubHandler handler;
    private State state;

    @BeforeEach
    void setUp() throws Exception {
        requestHookExecuted = new AtomicInteger();
//        ircBot = mock(IrcBot.class);
        BotStorage botStorage = new InMemoryBotStorage();
        StateStorage stateStorage = new InMemoryStateStorage();
        handler = new SendingYouPackNoticeMessageHandler(botStorage, stateStorage);

        initializeStorages(botStorage, stateStorage);
    }

    private void initializeStorages(BotStorage botStorage, StateStorage stateStorage) {
        botStorage.save("Andy", ircBot);

        Pack pack = testPack();
        Runnable requestHook = () -> requestHookExecuted.incrementAndGet();
        state = new State(pack, requestHook);
        stateStorage.save("Andy", state);
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
    void incoming_pack_matches_requested_pack() {
        String noticeMessage = "** Sending you pack #1 (\"test1.bin\"), which is <1kB. (resume supported)";
        NoticeMessageCommand command = new NoticeMessageCommand("Andy", "keex", noticeMessage);

        boolean handled = handler.handle(command);

        assertTrue(handled);
        assertTrue(state.isRemoteSendsCorrectPack());
        assertTrue(state.isPackResumable());
        verifyNoInteractions(ircBot);
    }

    @Test
    void incoming_pack_matches_requested_pack_not_resumable() {
        String noticeMessage = "** Sending you pack #1 (\"test1.bin\"), which is <1kB.";
        NoticeMessageCommand command = new NoticeMessageCommand("Andy", "keex", noticeMessage);

        boolean handled = handler.handle(command);

        assertTrue(handled);
        assertTrue(state.isRemoteSendsCorrectPack());
        assertFalse(state.isPackResumable());
        verifyNoInteractions(ircBot);
    }

    @Test
    void incoming_pack_does_not_match_requested_pack() {
        String noticeMessage = "** Sending you pack #1 (\"some_other.bin\"), which is <1kB. (resume supported)";
        NoticeMessageCommand command = new NoticeMessageCommand("Andy", "keex", noticeMessage);

        boolean handled = handler.handle(command);

        assertTrue(handled);
        assertFalse(state.isRemoteSendsCorrectPack());
        verify(ircBot).startSearchListing("keex", "test1.bin");
    }
}
