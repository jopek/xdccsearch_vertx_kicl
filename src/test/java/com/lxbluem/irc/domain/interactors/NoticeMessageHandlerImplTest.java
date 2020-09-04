package com.lxbluem.irc.domain.interactors;

import com.lxbluem.common.domain.Pack;
import com.lxbluem.common.domain.events.*;
import com.lxbluem.common.domain.ports.BotMessaging;
import com.lxbluem.common.domain.ports.EventDispatcher;
import com.lxbluem.irc.adapters.InMemoryBotStateStorage;
import com.lxbluem.irc.adapters.InMemoryBotStorage;
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
import org.mockito.Captor;
import org.mockito.runners.MockitoJUnitRunner;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class NoticeMessageHandlerImplTest {

    private BotStateStorage stateStorage;
    private BotMessaging botMessaging;
    private EventDispatcher eventDispatcher;
    private IrcBot ircBot;
    private BotStorage botStorage;
    private final Instant fixedInstant = Instant.parse("2020-08-10T10:11:22Z");

    @Captor
    private ArgumentCaptor<Collection<String>> stringCollectionCaptor;

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

        noticeMessageHandler = new NoticeMessageHandlerImpl(botStorage, stateStorage, eventDispatcher, clock, exitBot);
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
    public void notice_message_handler_nickserv_register_nick() {
        String botNick = "Andy";
        String remoteNick = "nickserv";
        String noticeMessage = "your nickname is not registered. to register it, use";

        noticeMessageHandler.handle(new NoticeMessageCommand(botNick, remoteNick, noticeMessage));

        verify(ircBot).registerNickname(botNick);
        verifyNoMoreInteractions(botMessaging, ircBot, eventDispatcher);
    }

    @Test
    public void notice_message_handler_nickserv_registered_nick_request() {
        String botNick = "Andy";
        String remoteNick = "nickserv";
        String noticeMessage = "nickname Andy registered";
        Pack pack = testPack();

        assertTrue(stateStorage.get("Andy").isPresent());
        BotState botState = stateStorage.get("Andy").get();
        botState.nickRegistryRequired();
        botState.channelNickList(pack.getChannelName(), Collections.singletonList(pack.getNickName()));
        botState.channelReferences(pack.getChannelName(), Arrays.asList());

        noticeMessageHandler.handle(new NoticeMessageCommand(botNick, remoteNick, noticeMessage));

        verify(ircBot, never()).registerNickname(botNick);
        verifyNoMoreInteractions(botMessaging, ircBot, eventDispatcher);
        assertEquals(1, requestHookExecuted.get());
    }

    @Test
    public void notice_message_handler_more_channels_required() {
        String botNick = "Andy";
        String remoteNick = "Zombie";
        String noticeMessage = "[#DOWNLOAD] \u00034!!!WARNING!!! YOU MUST IDLE IN #ZW-CHAT - IF YOU ATTEMPT TO DOWNLOAD WITHOUT BEING IN #ZW-CHAT YOU WILL BE BANNED!\n";
        Pack pack = testPack();

        assertTrue(stateStorage.get("Andy").isPresent());
        BotState botState = stateStorage.get("Andy").get();

        botState.channelReferences(pack.getChannelName(), Arrays.asList());

        noticeMessageHandler.handle(new NoticeMessageCommand(botNick, remoteNick, noticeMessage));

        verify(ircBot, never()).registerNickname(botNick);
        verify(ircBot).joinChannel(new HashSet<>(Collections.singletonList("#zw-chat")));

        verifyNoMoreInteractions(botMessaging, ircBot, eventDispatcher);
    }

    @Test
    public void notice_message_handler_more_channels_required_after_request() {
        String botNick = "Andy";
        String remoteNick = "Zombie";
        String noticeMessage = "[#DOWNLOAD] \u00034!!!WARNING!!! YOU MUST IDLE IN #ZW-CHAT - IF YOU ATTEMPT TO DOWNLOAD WITHOUT BEING IN #ZW-CHAT YOU WILL BE BANNED!\n";
        Pack pack = testPack();

        assertTrue(stateStorage.get("Andy").isPresent());
        BotState botState = stateStorage.get("Andy").get();
        botState.channelReferences("#download", Arrays.asList("#someChannel"));
        botState.channelNickList("#download", Collections.singletonList(pack.getNickName()));
        botState.channelNickList("#someChannel", Arrays.asList("user1", "user2"));

        assertEquals(1, requestHookExecuted.get());

        assertTrue(botState.hasRequestedPack());
        assertFalse(botState.canRequestPack());

        noticeMessageHandler.handle(new NoticeMessageCommand(botNick, remoteNick, noticeMessage));

        verify(ircBot, never()).registerNickname(botNick);
        verify(ircBot).joinChannel(stringCollectionCaptor.capture());
        assertEquals(1, stringCollectionCaptor.getValue().size());
        assertTrue(stringCollectionCaptor.getValue().contains("#zw-chat"));

        assertFalse(botState.canRequestPack());
        verifyNoMoreInteractions(botMessaging, ircBot, eventDispatcher);
    }

    @Test
    public void notice_message_handler_connection_refused() {
        String botNick = "Andy";
        String remoteNick = "keex";
        String noticeMessage = "connection refused";

        noticeMessageHandler.handle(new NoticeMessageCommand(botNick, remoteNick, noticeMessage));

        ArgumentCaptor<BotEvent> messageSentCaptor = ArgumentCaptor.forClass(BotEvent.class);
        verify(eventDispatcher, times(2)).dispatch(messageSentCaptor.capture());
        List<BotEvent> eventList = messageSentCaptor.getAllValues();

        BotFailedEvent failMessage = (BotFailedEvent) eventList.get(0);
        assertEquals("Andy", failMessage.getBot());
        assertEquals("connection refused", failMessage.getMessage());
        assertEquals(fixedInstant.toEpochMilli(), failMessage.getTimestamp());

        BotExitedEvent exitMessage = (BotExitedEvent) eventList.get(1);
        assertEquals("Andy", exitMessage.getBot());
        assertEquals("Bot Andy exiting because connection refused", exitMessage.getMessage());
        assertEquals(fixedInstant.toEpochMilli(), exitMessage.getTimestamp());

        verify(ircBot).cancelDcc("keex");
        verify(ircBot).terminate();
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