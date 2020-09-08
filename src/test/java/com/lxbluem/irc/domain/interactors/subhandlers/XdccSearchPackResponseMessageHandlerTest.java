package com.lxbluem.irc.domain.interactors.subhandlers;

import com.lxbluem.common.domain.Pack;
import com.lxbluem.common.domain.events.BotNoticeEvent;
import com.lxbluem.common.domain.ports.EventDispatcher;
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
import org.mockito.ArgumentCaptor;

import java.time.Clock;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.Assert.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

public class XdccSearchPackResponseMessageHandlerTest {

    private EventDispatcher eventDispatcher;
    private IrcBot ircBot;
    private AtomicInteger requestHookExecuted;
    private NoticeMessageHandler.SubHandler noticeMessageHandler;
    private BotState botState;


    @Before
    public void setUp() {
        requestHookExecuted = new AtomicInteger();
        Clock clock = Clock.systemDefaultZone();
        ircBot = mock(IrcBot.class);
        BotStorage botStorage = new InMemoryBotStorage();
        BotStateStorage stateStorage = new InMemoryBotStateStorage();
        eventDispatcher = mock(EventDispatcher.class);
        noticeMessageHandler = new XdccSearchPackResponseMessageHandler(botStorage, stateStorage, eventDispatcher, clock);

        initialiseStorages(botStorage, stateStorage);
    }

    private void initialiseStorages(BotStorage botStorage, BotStateStorage stateStorage) {
        botStorage.save("Andy", ircBot);

        Pack pack = testPack();
        Runnable requestHook = () -> requestHookExecuted.addAndGet(1);
        botState = new BotState(pack, requestHook);
        stateStorage.save("Andy", botState);
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
    public void search_request_response() {
        botState.requestSearchListing();
        assertTrue(botState.isSearchRequested());

        noticeMessageHandler.handle(new NoticeMessageCommand("Andy", "keex", "Searching for \"test\"..."));
        noticeMessageHandler.handle(new NoticeMessageCommand("Andy", "keex", " - Pack #1 matches, \"test1.bin\""));

        assertFalse(botState.isSearchRequested());
        verify(ircBot).stopSearchListing("keex");
        verify(ircBot).requestDccPack("keex", 1);
        assertEquals("test1.bin", botState.getPack().getPackName());
        assertEquals(1, botState.getPack().getPackNumber());

        ArgumentCaptor<BotNoticeEvent> messageSent = ArgumentCaptor.forClass(BotNoticeEvent.class);
        verify(eventDispatcher).dispatch(messageSent.capture());
        BotNoticeEvent event = messageSent.getValue();
        assertEquals("Andy", event.getBot());
        assertEquals("keex", event.getRemoteNick());
        assertEquals("pack number changed #5 -> #1; requesting #1", event.getMessage());
    }

}