package com.lxbluem.irc.domain;

import com.lxbluem.common.domain.Pack;
import com.lxbluem.common.domain.events.*;
import com.lxbluem.common.domain.ports.BotMessaging;
import com.lxbluem.common.domain.ports.EventDispatcher;
import com.lxbluem.common.infrastructure.Address;
import com.lxbluem.irc.adapters.InMemoryBotStateStorage;
import com.lxbluem.irc.adapters.InMemoryBotStorage;
import com.lxbluem.irc.domain.exception.BotNotFoundException;
import com.lxbluem.irc.domain.model.BotState;
import com.lxbluem.irc.domain.model.request.DccCtcpQuery;
import com.lxbluem.irc.domain.model.request.DccInitializeRequest;
import com.lxbluem.irc.domain.model.request.FilenameResolveRequest;
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
import java.util.function.Consumer;

import static java.util.Arrays.asList;
import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class BotServiceTest {

    private BotStateStorage stateStorage;
    private BotService botService;
    private BotMessaging botMessaging;
    private EventDispatcher eventDispatcher;
    private IrcBot ircBot;
    private BotStorage botStorage;
    private final Instant fixedInstant = Instant.parse("2020-08-10T10:11:22Z");

    @Captor
    private ArgumentCaptor<Collection<String>> stringCollectionCaptor;
    @Captor
    private ArgumentCaptor<Consumer<Map<String, Object>>> consumerArgumentCaptor;
    private final NameGenerator nameGenerator = mock(NameGenerator.class);
    private final AtomicInteger requestHookExecuted = new AtomicInteger();


    @Before
    public void setUp() {
        botMessaging = mock(BotMessaging.class);
        ircBot = mock(IrcBot.class);
        stateStorage = new InMemoryBotStateStorage();
        botStorage = new InMemoryBotStorage();
        Clock clock = Clock.fixed(fixedInstant, ZoneId.systemDefault());
        when(nameGenerator.getNick()).thenReturn("Andy");
        eventDispatcher = mock(EventDispatcher.class);
        initialiseStorages();
        requestHookExecuted.set(0);

        botService = new BotService(
                botStorage,
                stateStorage,
                botMessaging,
                eventDispatcher,
                clock,
                nameGenerator
        );
    }

    private void initialiseStorages() {
        botStorage.save("Andy", ircBot);

        Pack pack = testPack();
        Runnable requestHook = () -> requestHookExecuted.addAndGet(1);
        stateStorage.save("Andy", new BotState(pack, requestHook));
    }

    @Test
    public void exit_before_executing() {
        assertTrue(stateStorage.get("Andy").isPresent());
        BotState state = stateStorage.get("Andy").get();
        state.channelReferences("#download", Arrays.asList());

        botService.manualExit("Andy");

        verify(ircBot).cancelDcc("keex");
        verify(ircBot).terminate();
        verify(eventDispatcher, times(1)).dispatch(any(BotExitedEvent.class));
        verifyZeroInteractions(ircBot, botMessaging, eventDispatcher);

        state.channelNickList("#download", Arrays.asList("keex", "user2", "user3"));

        assertEquals(1, requestHookExecuted.get());
    }

    @Test
    public void terminte_bot_manually() {
        botService.manualExit("Andy");

        verify(ircBot).cancelDcc("keex");
        verify(ircBot).terminate();

        assertFalse(botStorage.get("Andy").isPresent());
        assertFalse(stateStorage.get("Andy").isPresent());

        ArgumentCaptor<BotExitedEvent> messageSentCaptor = ArgumentCaptor.forClass(BotExitedEvent.class);
        verify(eventDispatcher).dispatch(messageSentCaptor.capture());

        verifyNoMoreInteractions(botMessaging, ircBot, eventDispatcher);

        BotExitedEvent sentMesssage = messageSentCaptor.getValue();
        assertEquals("Andy", sentMesssage.getBot());
        assertEquals("Bot Andy exiting because requested shutdown", sentMesssage.getMessage());
        assertEquals(fixedInstant.toEpochMilli(), sentMesssage.getTimestamp());
        assertFalse(stateStorage.get("Andy").isPresent());
        assertEquals("Andy", sentMesssage.getBot());
        assertEquals(fixedInstant.toEpochMilli(), sentMesssage.getTimestamp());
    }

    @Test(expected = BotNotFoundException.class)
    public void terminate_bot_manually_for_missing_bot() {
        botService.manualExit("nonexistent");
        verify(ircBot).terminate();

        verifyNoMoreInteractions(botMessaging, ircBot, eventDispatcher);
    }

    @Test
    public void terminate_bot() {
        botService.exit("Andy", "failure");
        verify(ircBot).cancelDcc("keex");
        verify(ircBot).terminate();

        assertFalse(botStorage.get("Andy").isPresent());
        assertFalse(stateStorage.get("Andy").isPresent());

        ArgumentCaptor<BotExitedEvent> messageSentCaptor = ArgumentCaptor.forClass(BotExitedEvent.class);
        verify(eventDispatcher).dispatch(messageSentCaptor.capture());
        verifyNoMoreInteractions(botMessaging, ircBot);

        BotExitedEvent sentMesssage = messageSentCaptor.getValue();
        assertEquals("Andy", sentMesssage.getBot());
        assertEquals("Bot Andy exiting because failure", sentMesssage.getMessage());
        assertEquals(fixedInstant.toEpochMilli(), sentMesssage.getTimestamp());
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
    public void users_in_channel() {
        botService.usersInChannel("Andy", "#download", asList("operator", "keex", "doomsman", "hellbaby"));

        verifyNoMoreInteractions(botMessaging, ircBot);

        assertEquals(1, requestHookExecuted.get());
        assertTrue(stateStorage.get("Andy").isPresent());
        BotState botState = stateStorage.get("Andy").get();
        assertTrue(botState.isRemoteUserSeen());
    }

    @Test
    public void users_in_channel__remoteUser_of_target_channel_missing() {
        botService.usersInChannel("Andy", "#download", asList("operator", "doomsman", "hellbaby"));

        ArgumentCaptor<BotEvent> messageSentCaptor = ArgumentCaptor.forClass(BotEvent.class);
        verify(eventDispatcher, times(2)).dispatch(messageSentCaptor.capture());
        List<BotEvent> eventList = messageSentCaptor.getAllValues();

        verify(ircBot).cancelDcc("keex");
        verify(ircBot).terminate();
        verifyNoMoreInteractions(botMessaging, ircBot, eventDispatcher);

        BotFailedEvent failedEvent = (BotFailedEvent) eventList.get(0);
        assertEquals("bot keex not in channel #download", failedEvent.getMessage());
        BotExitedEvent exitedEvent = (BotExitedEvent) eventList.get(1);
        assertEquals("Bot Andy exiting because bot keex not in channel #download", exitedEvent.getMessage());
        assertFalse(stateStorage.get("Andy").isPresent());
        assertFalse(botStorage.get("Andy").isPresent());
    }

    @Test
    public void channel_topic() {
        botService.channelTopic("Andy", "#download", "join #room; for #help, otherwise [#voice] ");

        verify(ircBot).joinChannel(stringCollectionCaptor.capture());
        Collection<String> channelsToJoin = stringCollectionCaptor.getValue();
        assertEquals(2, channelsToJoin.size());
        assertTrue(channelsToJoin.containsAll(Arrays.asList("#voice", "#room")));
        verifyNoMoreInteractions(botMessaging, ircBot, eventDispatcher);
    }

    @Test
    public void channel_topic__no_other_channels_referenced() {
        botService.channelTopic("Andy", "#download", "lalalal");

        verify(ircBot).joinChannel(stringCollectionCaptor.capture());
        assertTrue(stringCollectionCaptor.getValue().isEmpty());
        verifyNoMoreInteractions(botMessaging, ircBot, eventDispatcher);
    }

    @Test
    public void message_of_the_day() {
        botService.messageOfTheDay("Andy", asList("message of the", "dayyyyyy", "in multiple strings"));

        verify(ircBot).registerNickname("Andy");
        verifyNoMoreInteractions(botMessaging, ircBot, eventDispatcher);
    }

    @Test
    public void register_new_nick_when_rejected() {
        when(nameGenerator.getNick()).thenReturn("Randy");
        botService.changeNick("Andy", "something happened; serverMessages; more serverMessages");

        verify(ircBot).changeNickname(eq("Randy"));

        ArgumentCaptor<BotRenamedEvent> messageSentCaptor = ArgumentCaptor.forClass(BotRenamedEvent.class);
        verify(eventDispatcher).dispatch(messageSentCaptor.capture());

        BotRenamedEvent sentMesssage = messageSentCaptor.getValue();
        assertEquals("Andy", sentMesssage.getBot());
        assertEquals("something happened; serverMessages; more serverMessages", sentMesssage.getMessage());
        assertEquals("Randy", sentMesssage.getRenameto());
        assertEquals(fixedInstant.toEpochMilli(), sentMesssage.getTimestamp());

        verifyNoMoreInteractions(botMessaging, ircBot, eventDispatcher);
    }

    @Test
    public void notice_message_handler_send_other_messages_via_notice() {
        String botNick = "Andy";
        String remoteNick = "someDude";
        String noticeMessage = "lalala";

        botService.handleNoticeMessage(botNick, remoteNick, noticeMessage);

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

        botService.handleNoticeMessage(botNick, remoteNick, noticeMessage);

        verify(ircBot).registerNickname(botNick);
        verifyNoMoreInteractions(botMessaging, ircBot, eventDispatcher);
    }

    @Test
    public void notice_message_handler_account_register_nick() {
        BotState botState = stateStorage.get("Andy").get();
        String botNick = "Andy";
        String numericCommandMessage = "You need to be identified to a registered account to join this channel";
        botState.channelReferences("#download", Arrays.asList("#2", "#mg-lounge"));
        botState.channelNickList("#download", Arrays.asList("keex"));
        botState.channelReferences("#2", Collections.emptyList());

        botService.channelRequiresAccountRegistry(botNick, "#mg-lounge", numericCommandMessage);

        assertEquals(1, requestHookExecuted.get());
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

        botService.handleNoticeMessage(botNick, remoteNick, noticeMessage);

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

        botService.handleNoticeMessage(botNick, remoteNick, noticeMessage);

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

        botService.handleNoticeMessage(botNick, remoteNick, noticeMessage);

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

        assertTrue(stateStorage.get("Andy").isPresent());
        BotState botState = stateStorage.get("Andy").get();
        int botStateHash = botState.hashCode();

        botService.handleNoticeMessage(botNick, remoteNick, noticeMessage);
        int botStateHashAfterMethod = botState.hashCode();

        assertEquals("bot state was altered in the notice message handler", botStateHash, botStateHashAfterMethod);

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

        botService.handleNoticeMessage(botNick, remoteNick, noticeMessage);

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

    @Test
    public void incoming_invalid_ctcp_query() {
        String botNick = "Andy";
        String incoming_message = "crrrrrap";
        DccCtcpQuery ctcpQuery = DccCtcpQuery.fromQueryString(incoming_message);

        botService.handleCtcpQuery(botNick, ctcpQuery, 0L);

        verifyNoMoreInteractions(botMessaging, ircBot, eventDispatcher);
    }

    @Test
    public void incoming_ctcp_query_active_dcc() {
        String botNick = "Andy";
        // DCC SEND <filename> <ip> <port> <file size>
        String incoming_message = "DCC SEND test1.bin 3232260964 50000 6";
        DccCtcpQuery ctcpQuery = DccCtcpQuery.fromQueryString(incoming_message);

        botService.handleCtcpQuery(botNick, ctcpQuery, 0L);

        verify(botMessaging).ask(eq(Address.FILENAME_RESOLVE), eq(new FilenameResolveRequest("test1.bin")), consumerArgumentCaptor.capture());
        Consumer<Map<String, Object>> resolvedFilenameConsumer = consumerArgumentCaptor.getValue();
        resolvedFilenameConsumer.accept(Collections.singletonMap("filename", "test1._x0x_.bin"));

        DccInitializeRequest query = DccInitializeRequest.from(ctcpQuery, botNick);
        verify(botMessaging).ask(eq(Address.DCC_INITIALIZE), eq(query), consumerArgumentCaptor.capture());
        Consumer<Map<String, Object>> dccInitConsumer = consumerArgumentCaptor.getValue();
        dccInitConsumer.accept(Collections.emptyMap());

        verifyNoMoreInteractions(botMessaging, ircBot, eventDispatcher);
    }

    @Test
    public void incoming_ctcp_query_passive_dcc() {
        String botNick = "Andy";
        // DCC SEND <filename> <ip> <port> <file size>
        String incoming_message = "DCC SEND test1.bin 3232260964 0 6 1";
        DccCtcpQuery ctcpQuery = DccCtcpQuery.fromQueryString(incoming_message);

        botService.handleCtcpQuery(botNick, ctcpQuery, 3232260865L);

        verify(botMessaging).ask(eq(Address.FILENAME_RESOLVE), eq(new FilenameResolveRequest("test1.bin")), consumerArgumentCaptor.capture());
        Consumer<Map<String, Object>> resolvedFilenameConsumer = consumerArgumentCaptor.getValue();
        resolvedFilenameConsumer.accept(Collections.singletonMap("filename", "test1._x0x_.bin"));

        DccInitializeRequest query = DccInitializeRequest.from(ctcpQuery, botNick);
        verify(botMessaging).ask(eq(Address.DCC_INITIALIZE), eq(query), consumerArgumentCaptor.capture());
        Consumer<Map<String, Object>> dccInitConsumer = consumerArgumentCaptor.getValue();
        dccInitConsumer.accept(Collections.singletonMap("port", 12345));

        verify(ircBot).sendCtcpMessage("keex", "DCC SEND test1._x0x_.bin 3232260865 12345 6 1");

        verifyNoMoreInteractions(botMessaging, ircBot, eventDispatcher);
    }
}