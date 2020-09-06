package com.lxbluem.irc.domain.interactors;

import com.lxbluem.common.domain.Pack;
import com.lxbluem.common.domain.events.BotEvent;
import com.lxbluem.common.domain.events.BotNoticeEvent;
import com.lxbluem.common.domain.events.DccQueuedEvent;
import com.lxbluem.common.domain.ports.BotMessaging;
import com.lxbluem.common.domain.ports.EventDispatcher;
import com.lxbluem.irc.adapters.InMemoryBotStateStorage;
import com.lxbluem.irc.adapters.InMemoryBotStorage;
import com.lxbluem.irc.domain.interactors.subhandlers.*;
import com.lxbluem.irc.domain.model.BotState;
import com.lxbluem.irc.domain.model.request.NoticeMessageCommand;
import com.lxbluem.irc.domain.ports.incoming.ExitBot;
import com.lxbluem.irc.domain.ports.outgoing.BotStateStorage;
import com.lxbluem.irc.domain.ports.outgoing.BotStorage;
import com.lxbluem.irc.domain.ports.outgoing.IrcBot;
import com.lxbluem.irc.domain.ports.outgoing.NameGenerator;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.runners.MockitoJUnitRunner;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class NoticeMessageHandlerImplTest {

    private BotStateStorage stateStorage;
    private BotMessaging botMessaging;
    private EventDispatcher eventDispatcher;
    private IrcBot ircBot;
    private BotStorage botStorage;
    private final Instant fixedInstant = Instant.parse("2020-08-10T10:11:22Z");


    private final NameGenerator nameGenerator = mock(NameGenerator.class);
    private final AtomicInteger requestHookExecuted = new AtomicInteger();
    private NoticeMessageHandlerImpl noticeMessageHandler;

    @Before
    public void setUp() {
        botMessaging = mock(BotMessaging.class);
        ircBot = mock(IrcBot.class);
        stateStorage = new InMemoryBotStateStorage();
        botStorage = new InMemoryBotStorage();
        when(nameGenerator.getNick()).thenReturn("Andy");
        eventDispatcher = mock(EventDispatcher.class);
        initialiseStorages();
        requestHookExecuted.set(0);
        Clock clock = Clock.fixed(fixedInstant, ZoneId.systemDefault());
        ExitBot exitBot = new ExitBotImpl(botStorage, stateStorage, eventDispatcher, clock);

        noticeMessageHandler = new NoticeMessageHandlerImpl(eventDispatcher, clock);
        noticeMessageHandler.registerMessageHandler(new FailureNoticeMessageHandler(botStorage, stateStorage, exitBot, eventDispatcher, clock));
        noticeMessageHandler.registerMessageHandler(new JoinMoreChannelsNoticeMessageHandler(botStorage, stateStorage));
        noticeMessageHandler.registerMessageHandler(new NickNameRegisteredNoticeMessageHandler(botStorage, stateStorage));
        noticeMessageHandler.registerMessageHandler(new QueuedNoticeMessageHandler(eventDispatcher, clock));
        noticeMessageHandler.registerMessageHandler(new RegisterNickNameNoticeMessageHandler(botStorage, stateStorage));
        noticeMessageHandler.registerMessageHandler(new XdccSearchPackResponseMessageHandler(botStorage, stateStorage, eventDispatcher, clock));
    }

    private void initialiseStorages() {
        botStorage.save("Andy", ircBot);

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
    public void notice_message_handler_send_other_messages_via_notice() {
        String botNick = "Andy";
        String remoteNick = "someDude";
        String noticeMessage = "lalala";

        noticeMessageHandler.handle(new NoticeMessageCommand(botNick, remoteNick, noticeMessage));

        ArgumentCaptor<BotNoticeEvent> captor = ArgumentCaptor.forClass(BotNoticeEvent.class);
        verify(eventDispatcher).dispatch(captor.capture());
        BotNoticeEvent botNoticeEvent = captor.getValue();
        assertEquals("someDude", botNoticeEvent.getRemoteNick());
        assertEquals("lalala", botNoticeEvent.getMessage());
        verifyNoMoreInteractions(botMessaging, ircBot, eventDispatcher);
    }

    @Test
    public void notice_message_handler_queued() {
        String botNick = "Andy";
        String remoteNick = "keex";
        String noticeMessage = "queue for pack";
        Pack pack = testPack();

        assertTrue(stateStorage.get("Andy").isPresent());
        BotState botState = stateStorage.get("Andy").get();
        botState.channelReferences(pack.getChannelName(), new HashSet<>());
        botState.channelNickList(pack.getChannelName(), Collections.singletonList(pack.getNickName()));

        noticeMessageHandler.handle(new NoticeMessageCommand(botNick, remoteNick, noticeMessage));

        ArgumentCaptor<BotEvent> messageSentCaptor = ArgumentCaptor.forClass(BotEvent.class);
        verify(eventDispatcher, times(1)).dispatch(messageSentCaptor.capture());
        List<BotEvent> eventList = messageSentCaptor.getAllValues();

        assertEquals(1, requestHookExecuted.get());

        DccQueuedEvent sentQueueMesssage = (DccQueuedEvent) eventList.get(0);
        assertEquals("Andy", sentQueueMesssage.getBot());
        assertEquals("queue for pack", sentQueueMesssage.getMessage());
        assertEquals(fixedInstant.toEpochMilli(), sentQueueMesssage.getTimestamp());

        verifyNoMoreInteractions(botMessaging, ircBot, eventDispatcher);
    }
}