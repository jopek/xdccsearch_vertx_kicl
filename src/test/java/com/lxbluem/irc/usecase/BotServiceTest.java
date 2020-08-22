package com.lxbluem.irc.usecase;

import com.lxbluem.Address;
import com.lxbluem.domain.Pack;
import com.lxbluem.domain.ports.BotMessaging;
import com.lxbluem.irc.NameGenerator;
import com.lxbluem.irc.adapter.InMemoryBotStateStorage;
import com.lxbluem.irc.adapter.InMemoryBotStorage;
import com.lxbluem.irc.domain.DccBotState;
import com.lxbluem.irc.domain.DefaultDccBotState;
import com.lxbluem.irc.usecase.exception.BotNotFoundException;
import com.lxbluem.irc.usecase.ports.BotPort;
import com.lxbluem.irc.usecase.ports.BotStorage;
import com.lxbluem.irc.usecase.ports.DccBotStateStorage;
import com.lxbluem.irc.usecase.requestmodel.*;
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

    private final Instant fixedInstant = Instant.parse("2020-08-10T10:11:22Z");
    private BotPort botPort;

    @Captor
    private ArgumentCaptor<Consumer<Map<String, Object>>> consumerArgumentCaptor;
    private final NameGenerator nameGenerator = mock(NameGenerator.class);


    @Before
    public void setUp() {
        stateStorage = new InMemoryBotStateStorage();
        botMessaging = mock(BotMessaging.class);
        botPort = mock(BotPort.class);
        BotFactory botFactory = service -> botPort;
        BotStorage botStorage = new InMemoryBotStorage();
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
        assertNull(stateStorage.getBotStateByNick("Andy"));

        botService.initializeBot(testPack());

        ArgumentCaptor<BotInitMessage> messageCaptor = ArgumentCaptor.forClass(BotInitMessage.class);
        verify(botMessaging).notify(eq(Address.BOT_INIT), messageCaptor.capture());
        verify(botPort).connect(any(BotConnectionDetails.class));
        verify(botPort).joinChannel(eq("#download"));
        verifyNoMoreInteractions(botMessaging, botPort);
        assertEquals(testPack(), messageCaptor.getValue().getPack());
        assertNotNull(stateStorage.getBotStateByNick("Andy"));
        assertEquals(testPack(), stateStorage.getBotStateByNick("Andy").getPack());
    }

    @Test
    public void mark_channel_joined() {
        botService.initializeBot(testPack());
        reset(botMessaging, botPort);

        botService.onRequestedChannelJoinComplete("Andy", "#download");
        verifyNoMoreInteractions(botMessaging, botPort);

        DccBotState dccBotState = stateStorage.getBotStateByNick("Andy");
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

        DccBotState dccBotState = stateStorage.getBotStateByNick("Andy");
        assertTrue(((DefaultDccBotState) dccBotState).isRemoteUserSeen());
    }

    @Test
    public void channel_topic() {
        botService.initializeBot(testPack());
        reset(botMessaging, botPort);

        botService.channelTopic("Andy", "#download", "join #room; for #help, otherwise [#voice] ");

        HashSet<String> channelReferences = new HashSet<>(asList("#room", "#voice"));
        DccBotState dccBotState = stateStorage.getBotStateByNick("Andy");
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

        ArgumentCaptor<BotRenameMessage> messageSentCaptor = ArgumentCaptor.forClass(BotRenameMessage.class);
        verify(botMessaging).notify(eq(Address.BOT_UPDATE_NICK), messageSentCaptor.capture());

        BotRenameMessage sentMesssage = messageSentCaptor.getValue();
        assertEquals("Andy", sentMesssage.getBot());
        assertEquals("something happened; serverMessages; more serverMessages", sentMesssage.getMessage());
        assertEquals("Randy", sentMesssage.getNewBotName());
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

        ArgumentCaptor<BotNoticeMessage> captor = ArgumentCaptor.forClass(BotNoticeMessage.class);
        verify(botMessaging).notify(eq(Address.BOT_NOTICE), captor.capture());

        BotNoticeMessage botNoticeMessage = captor.getValue();
        assertEquals("someDude", botNoticeMessage.getRemoteNick());
        assertEquals("lalala", botNoticeMessage.getMessage());

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

        DccBotState botState = stateStorage.getBotStateByNick(botNick);
        botState.nickRegistryRequired();
        botState.channelNickList(pack.getChannelName(), Collections.singletonList(pack.getNickName()));
        botState.joinedChannel(pack.getChannelName());

        botService.handleNoticeMessage(botNick, remoteNick, noticeMessage);

        verify(botPort, never()).registerNickname(botNick);
        verify(botPort).requestDccPack("keex", 5);

        ArgumentCaptor<BotNoticeMessage> messageSentCaptor = ArgumentCaptor.forClass(BotNoticeMessage.class);
        verify(botMessaging).notify(eq(Address.BOT_NOTICE), messageSentCaptor.capture());

        BotNoticeMessage sentMesssage = messageSentCaptor.getValue();
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

        DccBotState botState = stateStorage.getBotStateByNick(botNick);
        int botStateHash = botState.hashCode();

        botService.handleNoticeMessage(botNick, remoteNick, noticeMessage);
        int botStateHashAfterMethod = botState.hashCode();

        assertEquals("bot state was altered in the notice message handler", botStateHash, botStateHashAfterMethod);

        ArgumentCaptor<BotFailMessage> messageSentCaptor = ArgumentCaptor.forClass(BotFailMessage.class);
        verify(botMessaging).notify(eq(Address.BOT_FAIL), messageSentCaptor.capture());

        BotFailMessage sentMesssage = messageSentCaptor.getValue();
        assertEquals("Andy", sentMesssage.getBot());
        assertEquals("connection refused", sentMesssage.getMessage());
        assertEquals(fixedInstant.toEpochMilli(), sentMesssage.getTimestamp());

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

        DccBotState botState = stateStorage.getBotStateByNick(botNick);
        botState.channelNickList(pack.getChannelName(), Collections.singletonList(pack.getNickName()));
        botState.joinedChannel(pack.getChannelName());

        botService.handleNoticeMessage(botNick, remoteNick, noticeMessage);

        verify(botPort, times(1)).requestDccPack("keex", 5);

        ArgumentCaptor<BotNoticeMessage> messageSentCaptor = ArgumentCaptor.forClass(BotNoticeMessage.class);
        verify(botMessaging).notify(eq(Address.BOT_NOTICE), messageSentCaptor.capture());

        BotNoticeMessage sentMesssage = messageSentCaptor.getValue();
        assertEquals("", sentMesssage.getRemoteNick());
        assertEquals("Andy", sentMesssage.getBot());
        assertEquals("requesting pack #5 from keex", sentMesssage.getMessage());
        assertEquals(fixedInstant.toEpochMilli(), sentMesssage.getTimestamp());

        ArgumentCaptor<BotDccQueueMessage> queueMessageSentCaptor = ArgumentCaptor.forClass(BotDccQueueMessage.class);
        verify(botMessaging).notify(eq(Address.BOT_DCC_QUEUE), queueMessageSentCaptor.capture());
        BotDccQueueMessage sentQueueMesssage = queueMessageSentCaptor.getValue();
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

        verify(botMessaging).ask(eq(Address.BOT_DCC_INIT), eq(ctcpQuery), consumerArgumentCaptor.capture());
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

        verify(botMessaging).ask(eq(Address.BOT_DCC_INIT), eq(ctcpQuery), consumerArgumentCaptor.capture());
        Consumer<Map<String, Object>> dccInitConsumer = consumerArgumentCaptor.getValue();
        dccInitConsumer.accept(Collections.singletonMap("port", 12345));

        verify(botPort).sendCtcpMessage("keex", "DCC SEND test1._x0x_.bin 3232260865 12345 6 1");
    }
}