package com.lxbluem.irc.domain.interactors.subhandlers;

import com.lxbluem.common.domain.Pack;
import com.lxbluem.common.domain.events.BotEvent;
import com.lxbluem.common.domain.events.DccQueuedEvent;
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

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.*;

public class QueuedNoticeMessageHandlerTest {
    private EventDispatcher eventDispatcher;
    private IrcBot ircBot;
    private AtomicInteger requestHookExecuted;
    private NoticeMessageHandler.SubHandler noticeMessageHandler;
    private BotState botState;

    @Before
    public void setUp() {
        requestHookExecuted = new AtomicInteger();
        ircBot = mock(IrcBot.class);
        BotStorage botStorage = new InMemoryBotStorage();
        BotStateStorage stateStorage = new InMemoryBotStateStorage();
        eventDispatcher = mock(EventDispatcher.class);

        noticeMessageHandler = new QueuedNoticeMessageHandler(eventDispatcher);

        initialiseStorages(botStorage, stateStorage);
    }

    private void initialiseStorages(BotStorage botStorage, BotStateStorage stateStorage) {
        botStorage.save("Andy", ircBot);

        Pack pack = testPack();
        Runnable requestHook = () -> requestHookExecuted.incrementAndGet();
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
                .build();
    }

    @Test
    public void notice_message_handler_queued() {
        String botNick = "Andy";
        String remoteNick = "keex";
        String noticeMessage = "queue for pack";
        Pack pack = testPack();

        botState.channelReferences("#download", new HashSet<>());
        botState.channelNickList("#download", Collections.singletonList(pack.getNickName()));

        noticeMessageHandler.handle(new NoticeMessageCommand(botNick, remoteNick, noticeMessage));

        ArgumentCaptor<BotEvent> messageSentCaptor = ArgumentCaptor.forClass(BotEvent.class);
        verify(eventDispatcher, times(1)).dispatch(messageSentCaptor.capture());
        List<BotEvent> eventList = messageSentCaptor.getAllValues();

        assertEquals(1, requestHookExecuted.get());

        DccQueuedEvent sentQueueMesssage = (DccQueuedEvent) eventList.get(0);
        assertEquals("Andy", sentQueueMesssage.getBot());
        assertEquals("queue for pack", sentQueueMesssage.getMessage());

        verifyNoMoreInteractions(ircBot, eventDispatcher);
    }
}