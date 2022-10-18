package com.lxbluem.irc.domain.interactors.subhandlers;

import com.lxbluem.common.domain.Pack;
import com.lxbluem.common.domain.events.BotNoticeEvent;
import com.lxbluem.common.domain.ports.EventDispatcher;
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
import org.mockito.ArgumentCaptor;

import java.time.Clock;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class XdccSearchPackResponseMessageHandlerTest {

    private EventDispatcher eventDispatcher;
    private IrcBot ircBot;
    private AtomicInteger requestHookExecuted;
    private NoticeMessageHandler.SubHandler noticeMessageHandler;
    private State state;


    @BeforeEach
    void setUp() {
        requestHookExecuted = new AtomicInteger();
        Clock clock = Clock.systemDefaultZone();
        ircBot = mock(IrcBot.class);
        BotStorage botStorage = new InMemoryBotStorage();
        StateStorage stateStorage = new InMemoryStateStorage();
        eventDispatcher = mock(EventDispatcher.class);
        noticeMessageHandler = new XdccSearchPackResponseMessageHandler(botStorage, stateStorage, eventDispatcher);

        initialiseStorages(botStorage, stateStorage);
    }

    private void initialiseStorages(BotStorage botStorage, StateStorage stateStorage) {
        botStorage.save("Andy", ircBot);

        Pack pack = testPack();
        Runnable requestHook = () -> requestHookExecuted.addAndGet(1);
        state = new State(pack, requestHook);
        stateStorage.save("Andy", state);
    }

    private Pack testPack() {
        return Pack.builder()
                .nickName("keex")
                .networkName("nn")
                .serverHostName("192.168.99.100")
                .serverPort(6667)
                .channelName("#download")
                .packNumber(5)
                .packName("test")
                .build();
    }


    @Test
    void search_request_response() {
        state.requestSearchListing();
        assertTrue(state.isSearchRequested());

        noticeMessageHandler.handle(new NoticeMessageCommand("Andy", "keex", "Searching for \"test\"..."));
        noticeMessageHandler.handle(new NoticeMessageCommand("Andy", "keex", " - Pack #1 matches, \"test1.bin\""));

        assertFalse(state.isSearchRequested());
        verify(ircBot).stopSearchListing("keex");
        verify(ircBot).requestDccPack("keex", 1);
        assertEquals("test1.bin", state.getPack().getPackName());
        assertEquals(1, state.getPack().getPackNumber());

        ArgumentCaptor<BotNoticeEvent> messageSent = ArgumentCaptor.forClass(BotNoticeEvent.class);
        verify(eventDispatcher).dispatch(messageSent.capture());
        BotNoticeEvent event = messageSent.getValue();
        assertEquals("Andy", event.getBot());
        assertEquals("keex", event.getRemoteNick());
        assertEquals("pack number changed #5 -> #1; requesting #1", event.getMessage());
    }

}
