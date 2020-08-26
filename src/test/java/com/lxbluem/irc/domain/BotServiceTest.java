package com.lxbluem.irc.domain;

import com.lxbluem.common.domain.Pack;
import com.lxbluem.common.domain.events.*;
import com.lxbluem.common.domain.ports.BotMessaging;
import com.lxbluem.common.infrastructure.Address;
import com.lxbluem.irc.adapters.InMemoryBotStateStorage;
import com.lxbluem.irc.adapters.InMemoryBotStorage;
import com.lxbluem.irc.domain.exception.BotNotFoundException;
import com.lxbluem.irc.domain.model.DccBotState;
import com.lxbluem.irc.domain.model.DefaultDccBotState;
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

    private DccBotStateStorage stateStorage;
    private BotService botService;
    private BotMessaging botMessaging;
    private BotPort botPort;
    private BotStorage botStorage;
    private final Instant fixedInstant = Instant.parse("2020-08-10T10:11:22Z");

    @Captor
    private ArgumentCaptor<Consumer<Map<String, Object>>> consumerArgumentCaptor;
    private final NameGenerator nameGenerator = mock(NameGenerator.class);


    @Before
    public void setUp() {
        botMessaging = mock(BotMessaging.class);
        botPort = mock(BotPort.class);
        BotFactory botFactory = service -> botPort;
        stateStorage = new InMemoryBotStateStorage();
        botStorage = new InMemoryBotStorage();
        Clock clock = Clock.fixed(fixedInstant, ZoneId.systemDefault());

        when(nameGenerator.getNick()).thenReturn("Andy");

        botService = new BotService(
                botStorage,
                stateStorage,
                botMessaging,
                botFactory,
                clock,
                nameGenerator
        );
    }

    @Test
    public void initialize_service_for_bot() {
        assertFalse(stateStorage.getBotStateByNick("Andy").isPresent());
        assertFalse(botStorage.getBotByNick("Andy").isPresent());

        botService.initializeBot(testPack());

        ArgumentCaptor<BotInitializedEvent> messageCaptor = ArgumentCaptor.forClass(BotInitializedEvent.class);
        verify(botMessaging).notify(eq(Address.BOT_INITIALIZED), messageCaptor.capture());
        verify(botPort).connect(any(BotConnectionDetails.class));
        verify(botPort).joinChannel(eq("#download"));
        verifyNoMoreInteractions(botMessaging, botPort);

        assertEquals(testPack(), messageCaptor.getValue().getPack());
        assertTrue(stateStorage.getBotStateByNick("Andy").isPresent());
        assertTrue(botStorage.getBotByNick("Andy").isPresent());
        assertEquals(testPack(), stateStorage.getBotStateByNick("Andy").get().getPack());
    }

    @Test
    public void exit_before_executing() {
        botService.initializeBot(testPack());
        verify(botPort).connect(any(BotConnectionDetails.class));
        verify(botPort).joinChannel(eq("#download"));
        verify(botMessaging).notify(eq(Address.BOT_INITIALIZED), any(BotInitializedEvent.class));

        DccBotState state = stateStorage.getBotStateByNick("Andy").get();

        state.joinedChannel("#download");
        state.channelReferences("#download", new HashSet<String>());

        botService.manualExit("Andy");
        verify(botPort).terminate();
        verify(botMessaging).notify(eq(Address.BOT_EXITED), any(BotExitedEvent.class));

        state.channelNickList("#download", Arrays.asList("keex", "user2", "user3"));
        verifyZeroInteractions(botPort, botMessaging);
    }

    @Test
    public void terminte_bot_manually() {
        botService.initializeBot(testPack());
        reset(botMessaging, botPort);

        botService.manualExit("Andy");
        verify(botPort).terminate();

        assertFalse(botStorage.getBotByNick("Andy").isPresent());
        assertFalse(stateStorage.getBotStateByNick("Andy").isPresent());

        ArgumentCaptor<BotExitedEvent> messageSentCaptor = ArgumentCaptor.forClass(BotExitedEvent.class);
        verify(botMessaging).notify(eq(Address.BOT_EXITED), messageSentCaptor.capture());

        BotExitedEvent sentMesssage = messageSentCaptor.getValue();
        assertEquals("Andy", sentMesssage.getBot());
        assertEquals("Bot Andy exiting because requested shutdown", sentMesssage.getMessage());
        assertEquals(fixedInstant.toEpochMilli(), sentMesssage.getTimestamp());

        assertFalse(stateStorage.getBotStateByNick("Andy").isPresent());
        verifyNoMoreInteractions(botMessaging, botPort);

        assertEquals("Andy", sentMesssage.getBot());
        assertEquals(fixedInstant.toEpochMilli(), sentMesssage.getTimestamp());
    }

    @Test(expected = BotNotFoundException.class)
    public void terminte_bot_manually_for_missing_bot() {
        botService.manualExit("Andy");
        verify(botPort).terminate();
    }

    @Test
    public void terminte_bot() {
        botService.initializeBot(testPack());
        reset(botMessaging, botPort);

        botService.exit("Andy", "failure");
        verify(botPort).terminate();

        assertFalse(botStorage.getBotByNick("Andy").isPresent());
        assertFalse(stateStorage.getBotStateByNick("Andy").isPresent());

        ArgumentCaptor<BotExitedEvent> messageSentCaptor = ArgumentCaptor.forClass(BotExitedEvent.class);
        verify(botMessaging).notify(eq(Address.BOT_EXITED), messageSentCaptor.capture());
        verifyNoMoreInteractions(botMessaging, botPort);

        BotExitedEvent sentMesssage = messageSentCaptor.getValue();
        assertEquals("Andy", sentMesssage.getBot());
        assertEquals("Bot Andy exiting because failure", sentMesssage.getMessage());
        assertEquals(fixedInstant.toEpochMilli(), sentMesssage.getTimestamp());
    }

    @Test
    public void mark_channel_joined() {
        botService.initializeBot(testPack());
        reset(botMessaging, botPort);

        botService.onRequestedChannelJoinComplete("Andy", "#download");
        verifyNoMoreInteractions(botMessaging, botPort);

        assertTrue(stateStorage.getBotStateByNick("Andy").isPresent());
        DccBotState dccBotState = stateStorage.getBotStateByNick("Andy").get();
        Set<String> joinedChannels = ((DefaultDccBotState) dccBotState).getJoinedChannels();
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
        reset(botMessaging, botPort);

        botService.usersInChannel("Andy", "#download", asList("operator", "keex", "doomsman", "hellbaby"));
        verifyNoMoreInteractions(botMessaging, botPort);

        assertTrue(stateStorage.getBotStateByNick("Andy").isPresent());
        DccBotState dccBotState = stateStorage.getBotStateByNick("Andy").get();
        assertTrue(((DefaultDccBotState) dccBotState).isRemoteUserSeen());
    }

    @Test
    public void users_in_channel__remoteUser_of_target_channel_missing() {
        botService.initializeBot(testPack());
        reset(botMessaging, botPort);

        botService.usersInChannel("Andy", "#download", asList("operator", "doomsman", "hellbaby"));

        ArgumentCaptor<BotFailedEvent> failMessageSentCaptor = ArgumentCaptor.forClass(BotFailedEvent.class);
        verify(botMessaging).notify(eq(Address.BOT_FAILED), failMessageSentCaptor.capture());
        ArgumentCaptor<BotExitedEvent> exitMessageSentCaptor = ArgumentCaptor.forClass(BotExitedEvent.class);
        verify(botMessaging).notify(eq(Address.BOT_EXITED), exitMessageSentCaptor.capture());
        verify(botPort).terminate();

        verifyNoMoreInteractions(botMessaging, botPort);

        assertEquals("bot keex not in channel #download", failMessageSentCaptor.getValue().getMessage());
        assertEquals("Bot Andy exiting because bot keex not in channel #download", exitMessageSentCaptor.getValue().getMessage());

        assertFalse(stateStorage.getBotStateByNick("Andy").isPresent());
        assertFalse(botStorage.getBotByNick("Andy").isPresent());
    }

    @Test
    public void channel_topic() {
        botService.initializeBot(testPack());
        reset(botMessaging, botPort);

        botService.channelTopic("Andy", "#download", "join #room; for #help, otherwise [#voice] ");

        HashSet<String> channelReferences = new HashSet<>(asList("#room", "#voice"));
        assertTrue(stateStorage.getBotStateByNick("Andy").isPresent());
        DccBotState dccBotState = stateStorage.getBotStateByNick("Andy").get();

        assertEquals(channelReferences, ((DefaultDccBotState) dccBotState).getReferencedChannelNames());
        verifyNoMoreInteractions(botMessaging, botPort);
    }

    @Test
    public void message_of_the_day() {
        botService.initializeBot(testPack());
        reset(botMessaging, botPort);

        botService.messageOfTheDay("Andy", asList("message of the", "dayyyyyy", "in multiple strings"));

        verify(botPort).registerNickname("Andy");
        verifyNoMoreInteractions(botMessaging, botPort);
    }

    @Test
    public void register_new_nick_when_rejected() {
        botService.initializeBot(testPack());
        reset(botMessaging, botPort);

        when(nameGenerator.getNick()).thenReturn("Randy");
        botService.changeNick("Andy", "something happened; serverMessages; more serverMessages");

        verify(botPort).changeNickname(eq("Randy"));

        ArgumentCaptor<BotRenamedEvent> messageSentCaptor = ArgumentCaptor.forClass(BotRenamedEvent.class);
        verify(botMessaging).notify(eq(Address.BOT_NICK_UPDATED), messageSentCaptor.capture());

        BotRenamedEvent sentMesssage = messageSentCaptor.getValue();
        assertEquals("Andy", sentMesssage.getBot());
        assertEquals("something happened; serverMessages; more serverMessages", sentMesssage.getMessage());
        assertEquals("Randy", sentMesssage.getRenameto());
        assertEquals(fixedInstant.toEpochMilli(), sentMesssage.getTimestamp());

        verifyNoMoreInteractions(botMessaging, botPort);
    }

    @Test
    public void notice_message_handler_send_other_messages_via_notice() {
        String botNick = "Andy";

        String remoteNick = "someDude";
        String noticeMessage = "lalala";

        botService.initializeBot(testPack());
        reset(botMessaging, botPort);

        botService.handleNoticeMessage(botNick, remoteNick, noticeMessage);

        ArgumentCaptor<BotNoticeEvent> captor = ArgumentCaptor.forClass(BotNoticeEvent.class);
        verify(botMessaging).notify(eq(Address.BOT_NOTICE), captor.capture());

        BotNoticeEvent botNoticeEvent = captor.getValue();
        assertEquals("someDude", botNoticeEvent.getRemoteNick());
        assertEquals("lalala", botNoticeEvent.getMessage());

        verifyNoMoreInteractions(botMessaging, botPort);
    }

    @Test
    public void notice_message_handler_nickserv_register_nick() {
        String botNick = "Andy";

        String remoteNick = "nickserv";
        String noticeMessage = "your nickname is not registered. to register it, use";

        botService.initializeBot(testPack());
        reset(botMessaging, botPort);

        botService.handleNoticeMessage(botNick, remoteNick, noticeMessage);
        verify(botPort).registerNickname(botNick);

        verifyNoMoreInteractions(botMessaging, botPort);
    }

    @Test
    public void notice_message_handler_nickserv_registered_nick_request() {
        String botNick = "Andy";
        String remoteNick = "nickserv";
        String noticeMessage = "nickname Andy registered";

        Pack pack = testPack();

        botService.initializeBot(pack);
        reset(botMessaging, botPort);

        assertTrue(stateStorage.getBotStateByNick("Andy").isPresent());
        DccBotState botState = stateStorage.getBotStateByNick("Andy").get();

        botState.nickRegistryRequired();
        botState.channelNickList(pack.getChannelName(), Collections.singletonList(pack.getNickName()));
        botState.joinedChannel(pack.getChannelName());

        botService.handleNoticeMessage(botNick, remoteNick, noticeMessage);

        verify(botPort, never()).registerNickname(botNick);
        verify(botPort).requestDccPack("keex", 5);

        ArgumentCaptor<BotNoticeEvent> messageSentCaptor = ArgumentCaptor.forClass(BotNoticeEvent.class);
        verify(botMessaging).notify(eq(Address.BOT_NOTICE), messageSentCaptor.capture());

        BotNoticeEvent sentMesssage = messageSentCaptor.getValue();
        assertEquals("", sentMesssage.getRemoteNick());
        assertEquals("Andy", sentMesssage.getBot());
        assertEquals("requesting pack #5 from keex", sentMesssage.getMessage());
        assertEquals(fixedInstant.toEpochMilli(), sentMesssage.getTimestamp());

        verifyNoMoreInteractions(botMessaging, botPort);
    }

    @Test
    public void notice_message_handler_connection_refused() {
        String botNick = "Andy";
        String remoteNick = "keex";
        String noticeMessage = "connection refused";

        Pack pack = testPack();

        botService.initializeBot(pack);
        reset(botMessaging, botPort);

        assertTrue(stateStorage.getBotStateByNick("Andy").isPresent());
        DccBotState botState = stateStorage.getBotStateByNick("Andy").get();
        int botStateHash = botState.hashCode();

        botService.handleNoticeMessage(botNick, remoteNick, noticeMessage);
        int botStateHashAfterMethod = botState.hashCode();

        assertEquals("bot state was altered in the notice message handler", botStateHash, botStateHashAfterMethod);

        ArgumentCaptor<BotFailedEvent> failMessageSentCaptor = ArgumentCaptor.forClass(BotFailedEvent.class);
        verify(botMessaging).notify(eq(Address.BOT_FAILED), failMessageSentCaptor.capture());

        BotFailedEvent failMessage = failMessageSentCaptor.getValue();
        assertEquals("Andy", failMessage.getBot());
        assertEquals("connection refused", failMessage.getMessage());
        assertEquals(fixedInstant.toEpochMilli(), failMessage.getTimestamp());

        ArgumentCaptor<BotExitedEvent> exitMessageSentCaptor = ArgumentCaptor.forClass(BotExitedEvent.class);
        verify(botMessaging).notify(eq(Address.BOT_EXITED), exitMessageSentCaptor.capture());
        BotExitedEvent exitMessage = exitMessageSentCaptor.getValue();

        assertEquals("Andy", exitMessage.getBot());
        assertEquals("Bot Andy exiting because connection refused", exitMessage.getMessage());
        assertEquals(fixedInstant.toEpochMilli(), exitMessage.getTimestamp());

        verify(botPort).terminate();
        verifyNoMoreInteractions(botMessaging, botPort);
    }

    @Test
    public void notice_message_handler_queued() {
        String botNick = "Andy";
        String remoteNick = "keex";
        String noticeMessage = "queue for pack";

        Pack pack = testPack();

        botService.initializeBot(pack);
        reset(botMessaging, botPort);

        assertTrue(stateStorage.getBotStateByNick("Andy").isPresent());
        DccBotState botState = stateStorage.getBotStateByNick("Andy").get();
        botState.channelNickList(pack.getChannelName(), Collections.singletonList(pack.getNickName()));
        botState.joinedChannel(pack.getChannelName());

        botService.handleNoticeMessage(botNick, remoteNick, noticeMessage);

        verify(botPort, times(1)).requestDccPack("keex", 5);

        ArgumentCaptor<BotNoticeEvent> messageSentCaptor = ArgumentCaptor.forClass(BotNoticeEvent.class);
        verify(botMessaging).notify(eq(Address.BOT_NOTICE), messageSentCaptor.capture());

        BotNoticeEvent sentMesssage = messageSentCaptor.getValue();
        assertEquals("", sentMesssage.getRemoteNick());
        assertEquals("Andy", sentMesssage.getBot());
        assertEquals("requesting pack #5 from keex", sentMesssage.getMessage());
        assertEquals(fixedInstant.toEpochMilli(), sentMesssage.getTimestamp());

        ArgumentCaptor<DccQueuedEvent> queueMessageSentCaptor = ArgumentCaptor.forClass(DccQueuedEvent.class);
        verify(botMessaging).notify(eq(Address.DCC_QUEUED), queueMessageSentCaptor.capture());
        DccQueuedEvent sentQueueMesssage = queueMessageSentCaptor.getValue();
        assertEquals("Andy", sentQueueMesssage.getBot());
        assertEquals("queue for pack", sentQueueMesssage.getMessage());
        assertEquals(fixedInstant.toEpochMilli(), sentQueueMesssage.getTimestamp());

        verifyNoMoreInteractions(botMessaging, botPort);
    }

    @Test
    public void incoming_invalid_ctcp_query() {
        String botNick = "Andy";
        String incoming_message = "crrrrrap";

        botService.initializeBot(testPack());
        reset(botMessaging, botPort);

        DccCtcpQuery ctcpQuery = DccCtcpQuery.fromQueryString(incoming_message);
        botService.handleCtcpQuery(botNick, ctcpQuery, 0L);
        verifyZeroInteractions(botMessaging, botPort);
    }

    @Test
    public void incoming_ctcp_query_active_dcc() {
        String botNick = "Andy";
        // DCC SEND <filename> <ip> <port> <file size>
        String incoming_message = "DCC SEND test1.bin 3232260964 50000 6";
        DccCtcpQuery ctcpQuery = DccCtcpQuery.fromQueryString(incoming_message);

        botService.initializeBot(testPack());
        reset(botMessaging, botPort);

        botService.handleCtcpQuery(botNick, ctcpQuery, 0L);

        verify(botMessaging).ask(eq(Address.FILENAME_RESOLVE), eq(new FilenameResolveRequest("test1.bin")), consumerArgumentCaptor.capture());
        Consumer<Map<String, Object>> resolvedFilenameConsumer = consumerArgumentCaptor.getValue();
        resolvedFilenameConsumer.accept(Collections.singletonMap("filename", "test1._x0x_.bin"));

        DccInitializeRequest query = DccInitializeRequest.from(ctcpQuery, botNick);
        verify(botMessaging).ask(eq(Address.DCC_INITIALIZE), eq(query), consumerArgumentCaptor.capture());
        Consumer<Map<String, Object>> dccInitConsumer = consumerArgumentCaptor.getValue();
        dccInitConsumer.accept(Collections.emptyMap());
    }

    @Test
    public void incoming_ctcp_query_passive_dcc() {
        String botNick = "Andy";
        // DCC SEND <filename> <ip> <port> <file size>
        String incoming_message = "DCC SEND test1.bin 3232260964 0 6 1";

        botService.initializeBot(testPack());
        reset(botMessaging, botPort);

        DccCtcpQuery ctcpQuery = DccCtcpQuery.fromQueryString(incoming_message);
        botService.handleCtcpQuery(botNick, ctcpQuery, 3232260865L);

        verify(botMessaging).ask(eq(Address.FILENAME_RESOLVE), eq(new FilenameResolveRequest("test1.bin")), consumerArgumentCaptor.capture());
        Consumer<Map<String, Object>> resolvedFilenameConsumer = consumerArgumentCaptor.getValue();
        resolvedFilenameConsumer.accept(Collections.singletonMap("filename", "test1._x0x_.bin"));

        DccInitializeRequest query = DccInitializeRequest.from(ctcpQuery, botNick);
        verify(botMessaging).ask(eq(Address.DCC_INITIALIZE), eq(query), consumerArgumentCaptor.capture());
        Consumer<Map<String, Object>> dccInitConsumer = consumerArgumentCaptor.getValue();
        dccInitConsumer.accept(Collections.singletonMap("port", 12345));

        verify(botPort).sendCtcpMessage("keex", "DCC SEND test1._x0x_.bin 3232260865 12345 6 1");
    }
}