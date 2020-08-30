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
import com.lxbluem.irc.domain.model.request.BotConnectionDetails;
import com.lxbluem.irc.domain.model.request.DccCtcpQuery;
import com.lxbluem.irc.domain.model.request.DccInitializeRequest;
import com.lxbluem.irc.domain.model.request.FilenameResolveRequest;
import com.lxbluem.irc.domain.ports.*;
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


    @Before
    public void setUp() {
        botMessaging = mock(BotMessaging.class);
        ircBot = mock(IrcBot.class);
        BotFactory botFactory = service -> ircBot;
        stateStorage = new InMemoryBotStateStorage();
        botStorage = new InMemoryBotStorage();
        Clock clock = Clock.fixed(fixedInstant, ZoneId.systemDefault());
        when(nameGenerator.getNick()).thenReturn("Andy");

        eventDispatcher = mock(EventDispatcher.class);

        botService = new BotService(
                botStorage,
                stateStorage,
                botMessaging,
                eventDispatcher,
                botFactory,
                clock,
                nameGenerator
        );
    }

    @Test
    public void initialize_service_for_bot() {
        assertFalse(stateStorage.get("Andy").isPresent());
        assertFalse(botStorage.get("Andy").isPresent());

        botService.initializeBot(testPack());

        ArgumentCaptor<Event> messageCaptor = ArgumentCaptor.forClass(Event.class);
        verify(eventDispatcher).dispatch(messageCaptor.capture());
        verify(ircBot).connect(any(BotConnectionDetails.class));
        verify(ircBot).joinChannel(eq("#download"));

        verifyNoMoreInteractions(botMessaging, ircBot);

        List<Event> dispatchedEvents = messageCaptor.getAllValues();
        assertEquals(testPack(), ((BotInitializedEvent) dispatchedEvents.get(0)).getPack());

        assertEquals(1, dispatchedEvents.size());
        assertEquals(BotInitializedEvent.class, dispatchedEvents.get(0).getClass());
        assertEquals(testPack(), ((BotInitializedEvent) dispatchedEvents.get(0)).getPack());

        assertTrue(stateStorage.get("Andy").isPresent());
        assertTrue(botStorage.get("Andy").isPresent());
        assertEquals(testPack(), stateStorage.get("Andy").get().getPack());
    }

    @Test
    public void exit_before_executing() {
        botService.initializeBot(testPack());
        verify(ircBot).connect(any(BotConnectionDetails.class));
        verify(ircBot).joinChannel(eq("#download"));
        verify(eventDispatcher).dispatch(any(BotInitializedEvent.class));

        BotState state = stateStorage.get("Andy").get();

        state.joinedChannel("#download");
        state.channelReferences("#download", new HashSet<String>());

        botService.manualExit("Andy");
        verify(ircBot).terminate();
        verify(eventDispatcher, times(2)).dispatch(any(BotExitedEvent.class));

        verifyZeroInteractions(ircBot, botMessaging, eventDispatcher);

        state.channelNickList("#download", Arrays.asList("keex", "user2", "user3"));
    }

    @Test
    public void terminte_bot_manually() {
        botService.initializeBot(testPack());
        reset(botMessaging, ircBot, eventDispatcher);

        botService.manualExit("Andy");
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
    public void terminte_bot_manually_for_missing_bot() {
        botService.manualExit("Andy");
        verify(ircBot).terminate();

        verifyNoMoreInteractions(botMessaging, ircBot, eventDispatcher);
    }

    @Test
    public void terminte_bot() {
        botService.initializeBot(testPack());
        reset(botMessaging, ircBot, eventDispatcher);

        botService.exit("Andy", "failure");
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

    @Test
    public void mark_channel_joined() {
        botService.initializeBot(testPack());
        reset(botMessaging, ircBot, eventDispatcher);

        botService.onRequestedChannelJoinComplete("Andy", "#download");
        verifyNoMoreInteractions(botMessaging, ircBot);

        assertTrue(stateStorage.get("Andy").isPresent());
        BotState botState = stateStorage.get("Andy").get();
        Set<String> joinedChannels = botState.getJoinedChannels();
        List<String> expectedJoinedChannels = Collections.singletonList("#download");
        assertTrue(joinedChannels.containsAll(expectedJoinedChannels));
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
        botService.initializeBot(testPack());
        reset(botMessaging, ircBot, eventDispatcher);

        botService.usersInChannel("Andy", "#download", asList("operator", "keex", "doomsman", "hellbaby"));
        verifyNoMoreInteractions(botMessaging, ircBot);

        assertTrue(stateStorage.get("Andy").isPresent());
        BotState botState = stateStorage.get("Andy").get();
        assertTrue(botState.isRemoteUserSeen());
    }

    @Test
    public void users_in_channel__remoteUser_of_target_channel_missing() {
        botService.initializeBot(testPack());
        reset(botMessaging, ircBot, eventDispatcher);

        botService.usersInChannel("Andy", "#download", asList("operator", "doomsman", "hellbaby"));

        ArgumentCaptor<BotEvent> messageSentCaptor = ArgumentCaptor.forClass(BotEvent.class);
        verify(eventDispatcher, times(2)).dispatch(messageSentCaptor.capture());
        List<BotEvent> eventList = messageSentCaptor.getAllValues();

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
        botService.initializeBot(testPack());
        reset(botMessaging, ircBot, eventDispatcher);

        botService.channelTopic("Andy", "#download", "join #room; for #help, otherwise [#voice] ");

        verify(ircBot).joinChannel(stringCollectionCaptor.capture());
        Collection<String> channelsToJoin = stringCollectionCaptor.getValue();
        assertEquals(2, channelsToJoin.size());
        assertTrue(channelsToJoin.containsAll(Arrays.asList("#voice", "#room")));
        verifyNoMoreInteractions(botMessaging, ircBot, eventDispatcher);
    }

    @Test
    public void channel_topic__no_other_channels_referenced() {
        botService.initializeBot(testPack());
        reset(botMessaging, ircBot, eventDispatcher);

        botService.channelTopic("Andy", "#download", "lalalal");

        verify(ircBot).joinChannel(stringCollectionCaptor.capture());
        assertTrue(stringCollectionCaptor.getValue().isEmpty());
        verifyNoMoreInteractions(botMessaging, ircBot, eventDispatcher);
    }

    @Test
    public void message_of_the_day() {
        botService.initializeBot(testPack());
        reset(botMessaging, ircBot, eventDispatcher);

        botService.messageOfTheDay("Andy", asList("message of the", "dayyyyyy", "in multiple strings"));

        verify(ircBot).registerNickname("Andy");
        verifyNoMoreInteractions(botMessaging, ircBot, eventDispatcher);
    }

    @Test
    public void register_new_nick_when_rejected() {
        botService.initializeBot(testPack());
        reset(botMessaging, ircBot, eventDispatcher);

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

        botService.initializeBot(testPack());
        reset(botMessaging, ircBot, eventDispatcher);

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

        botService.initializeBot(testPack());
        reset(botMessaging, ircBot, eventDispatcher);

        botService.handleNoticeMessage(botNick, remoteNick, noticeMessage);
        verify(ircBot).registerNickname(botNick);

        verifyNoMoreInteractions(botMessaging, ircBot, eventDispatcher);
    }

    @Test
    public void notice_message_handler_account_register_nick() {
        String botNick = "Andy";
        String numericCommandMessage = "You need to be identified to a registered account to join this channel";

        Pack pack = testPack();
        botService.initializeBot(pack);
        reset(botMessaging, ircBot, eventDispatcher);

        assertTrue(stateStorage.get("Andy").isPresent());
        BotState botState = stateStorage.get("Andy").get();

        botState.channelReferences("#download", Arrays.asList("#2", "#mg-lounge"));
        botState.channelNickList("#download", Arrays.asList("keex"));

        botState.channelReferences("#2", Collections.emptyList());

        botService.channelRequiresAccountRegistry(botNick, "#mg-lounge", numericCommandMessage);

        verify(ircBot).requestDccPack("keex", 5);
        verify(eventDispatcher).dispatch(any(BotNoticeEvent.class));

        verifyNoMoreInteractions(botMessaging, ircBot, eventDispatcher);
    }

    @Test
    public void notice_message_handler_nickserv_registered_nick_request() {
        String botNick = "Andy";
        String remoteNick = "nickserv";
        String noticeMessage = "nickname Andy registered";

        Pack pack = testPack();

        botService.initializeBot(pack);
        reset(botMessaging, ircBot, eventDispatcher);

        assertTrue(stateStorage.get("Andy").isPresent());
        BotState botState = stateStorage.get("Andy").get();

        botState.nickRegistryRequired();
        botState.channelNickList(pack.getChannelName(), Collections.singletonList(pack.getNickName()));
        botState.joinedChannel(pack.getChannelName());
        botState.channelReferences(pack.getChannelName(), new HashSet<>(Arrays.asList()));

        botService.handleNoticeMessage(botNick, remoteNick, noticeMessage);

        verify(ircBot, never()).registerNickname(botNick);
        verify(ircBot).requestDccPack("keex", 5);

        ArgumentCaptor<BotNoticeEvent> messageSentCaptor = ArgumentCaptor.forClass(BotNoticeEvent.class);
        verify(eventDispatcher).dispatch(messageSentCaptor.capture());

        BotNoticeEvent sentMesssage = messageSentCaptor.getValue();
        assertEquals("", sentMesssage.getRemoteNick());
        assertEquals("Andy", sentMesssage.getBot());
        assertEquals("requesting pack #5 from keex", sentMesssage.getMessage());
        assertEquals(fixedInstant.toEpochMilli(), sentMesssage.getTimestamp());

        verifyNoMoreInteractions(botMessaging, ircBot, eventDispatcher);
    }

    @Test
    public void notice_message_handler_more_channels_required() {
        String botNick = "Andy";
        String remoteNick = "Zombie";
        String noticeMessage = "[#DOWNLOAD] \u00034!!!WARNING!!! YOU MUST IDLE IN #ZW-CHAT - IF YOU ATTEMPT TO DOWNLOAD WITHOUT BEING IN #ZW-CHAT YOU WILL BE BANNED!\n";

        Pack pack = testPack();

        botService.initializeBot(pack);
        reset(botMessaging, ircBot, eventDispatcher);

        assertTrue(stateStorage.get("Andy").isPresent());
        BotState botState = stateStorage.get("Andy").get();

        botState.joinedChannel(pack.getChannelName());
        botState.channelReferences(pack.getChannelName(), new HashSet<>(Arrays.asList()));

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

        botService.initializeBot(pack);
        reset(botMessaging, ircBot, eventDispatcher);

        assertTrue(stateStorage.get("Andy").isPresent());
        BotState botState = stateStorage.get("Andy").get();

        String packChannelName = pack.getChannelName();
        botState.joinedChannel(packChannelName);
        botState.channelNickList(packChannelName, Collections.singletonList(pack.getNickName()));

        botState.channelReferences(packChannelName, new HashSet<>(Arrays.asList("#someChannel")));
        botState.joinedChannel("#someChannel");
        botState.channelNickList("#someChannel", Arrays.asList("user1", "user2"));
        verify(ircBot).requestDccPack(eq("keex"), eq(5));
        ArgumentCaptor<BotNoticeEvent> messageSentCaptor = ArgumentCaptor.forClass(BotNoticeEvent.class);
        verify(eventDispatcher).dispatch(messageSentCaptor.capture());
        BotNoticeEvent sentMesssage = messageSentCaptor.getValue();
        assertEquals("", sentMesssage.getRemoteNick());
        assertEquals("Andy", sentMesssage.getBot());
        assertEquals("requesting pack #5 from keex", sentMesssage.getMessage());

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

        Pack pack = testPack();

        botService.initializeBot(pack);
        reset(botMessaging, ircBot, eventDispatcher);

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

        verify(ircBot).terminate();
        verifyNoMoreInteractions(botMessaging, ircBot, eventDispatcher);
    }

    @Test
    public void notice_message_handler_queued() {
        String botNick = "Andy";
        String remoteNick = "keex";
        String noticeMessage = "queue for pack";

        Pack pack = testPack();

        botService.initializeBot(pack);
        reset(botMessaging, ircBot, eventDispatcher);

        assertTrue(stateStorage.get("Andy").isPresent());
        BotState botState = stateStorage.get("Andy").get();
        botState.joinedChannel(pack.getChannelName());
        botState.channelReferences(pack.getChannelName(), new HashSet<>());
        botState.channelNickList(pack.getChannelName(), Collections.singletonList(pack.getNickName()));

        botService.handleNoticeMessage(botNick, remoteNick, noticeMessage);

        verify(ircBot, times(1)).requestDccPack("keex", 5);

        ArgumentCaptor<BotEvent> messageSentCaptor = ArgumentCaptor.forClass(BotEvent.class);
        verify(eventDispatcher, times(2)).dispatch(messageSentCaptor.capture());
        List<BotEvent> eventList = messageSentCaptor.getAllValues();

        BotNoticeEvent sentMesssage = (BotNoticeEvent) eventList.get(0);
        assertEquals("", sentMesssage.getRemoteNick());
        assertEquals("Andy", sentMesssage.getBot());
        assertEquals("requesting pack #5 from keex", sentMesssage.getMessage());
        assertEquals(fixedInstant.toEpochMilli(), sentMesssage.getTimestamp());

        DccQueuedEvent sentQueueMesssage = (DccQueuedEvent) eventList.get(1);
        assertEquals("Andy", sentQueueMesssage.getBot());
        assertEquals("queue for pack", sentQueueMesssage.getMessage());
        assertEquals(fixedInstant.toEpochMilli(), sentQueueMesssage.getTimestamp());

        verifyNoMoreInteractions(botMessaging, ircBot, eventDispatcher);
    }

    @Test
    public void incoming_invalid_ctcp_query() {
        String botNick = "Andy";
        String incoming_message = "crrrrrap";

        botService.initializeBot(testPack());
        reset(botMessaging, ircBot, eventDispatcher);

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

        botService.initializeBot(testPack());
        reset(botMessaging, ircBot, eventDispatcher);

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

        botService.initializeBot(testPack());
        reset(botMessaging, ircBot, eventDispatcher);

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