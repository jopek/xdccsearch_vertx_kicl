package com.lxbluem.irc.domain.interactors;

import com.lxbluem.common.domain.Pack;
import com.lxbluem.common.domain.ports.BotMessaging;
import com.lxbluem.common.domain.ports.EventDispatcher;
import com.lxbluem.common.infrastructure.Address;
import com.lxbluem.irc.adapters.InMemoryBotStateStorage;
import com.lxbluem.irc.adapters.InMemoryBotStorage;
import com.lxbluem.irc.domain.model.BotState;
import com.lxbluem.irc.domain.model.request.DccCtcpQuery;
import com.lxbluem.irc.domain.model.request.DccInitializeRequest;
import com.lxbluem.irc.domain.model.request.FilenameResolveRequest;
import com.lxbluem.irc.domain.model.request.StartDccTransferCommand;
import com.lxbluem.irc.domain.ports.incoming.StartDccTransfer;
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

import java.util.Collections;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class StartDccTransferImplTest {
    private BotStateStorage stateStorage;
    private BotMessaging botMessaging;
    private EventDispatcher eventDispatcher;
    private IrcBot ircBot;
    private BotStorage botStorage;

    @Captor
    private ArgumentCaptor<Consumer<Map<String, Object>>> consumerArgumentCaptor;

    private final NameGenerator nameGenerator = mock(NameGenerator.class);
    private final AtomicInteger requestHookExecuted = new AtomicInteger();
    private StartDccTransfer startDccTransfer;

    @Before
    public void setUp() throws Exception {
        botMessaging = mock(BotMessaging.class);
        ircBot = mock(IrcBot.class);
        eventDispatcher = mock(EventDispatcher.class);

        stateStorage = new InMemoryBotStateStorage();
        botStorage = new InMemoryBotStorage();
        when(nameGenerator.getNick()).thenReturn("Andy");

        initialiseStorages();
        requestHookExecuted.set(0);

        startDccTransfer = new StartDccTransferImpl(botStorage, stateStorage, botMessaging);
    }

    private void initialiseStorages() {
        botStorage.save("Andy", ircBot);

        Pack pack = testPack();
        Runnable requestHook = requestHookExecuted::incrementAndGet;
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
                .packName("test1.bin")
                .build();
    }

    @Test
    public void incoming_invalid_ctcp_query() {
        String botNick = "Andy";
        String incoming_message = "crrrrrap";
        DccCtcpQuery ctcpQuery = DccCtcpQuery.fromQueryString(incoming_message);

        startDccTransfer.handle(new StartDccTransferCommand(botNick, ctcpQuery, 0L));

        verifyNoMoreInteractions(botMessaging, ircBot, eventDispatcher);
    }

    @Test
    public void incoming_ctcp_query_does_nothing_for_unexpected_filename() {
        String botNick = "Andy";
        // DCC SEND <filename> <ip> <port> <file size>
        String incoming_message = "DCC SEND unexpected_filename.txt 3232260964 50000 6";
        DccCtcpQuery ctcpQuery = DccCtcpQuery.fromQueryString(incoming_message);

        startDccTransfer.handle(new StartDccTransferCommand(botNick, ctcpQuery, 0L));

        verifyNoMoreInteractions(botMessaging, ircBot, eventDispatcher);
    }

    @Test
    public void incoming_ctcp_query_active_dcc() {
        String botNick = "Andy";
        // DCC SEND <filename> <ip> <port> <file size>
        String incoming_message = "DCC SEND test1.bin 3232260964 50000 6";
        DccCtcpQuery ctcpQuery = DccCtcpQuery.fromQueryString(incoming_message);

        startDccTransfer.handle(new StartDccTransferCommand(botNick, ctcpQuery, 0L));

        verify(botMessaging).ask(eq(Address.FILENAME_RESOLVE), eq(new FilenameResolveRequest("test1.bin")), consumerArgumentCaptor.capture());
        Consumer<Map<String, Object>> resolvedFilenameConsumer = consumerArgumentCaptor.getValue();
        resolvedFilenameConsumer.accept(Collections.singletonMap("filename", "test1._x0x_.bin"));

        DccInitializeRequest query = DccInitializeRequest.builder()
                .bot("Andy")
                .filename("test1._x0x_.bin")
                .ip("192.168.99.100")
                .port(50000)
                .passive(false)
                .size(6)
                .token(0)
                .build();
        verify(botMessaging).ask(eq(Address.DCC_INITIALIZE), eq(query), consumerArgumentCaptor.capture());
        Consumer<Map<String, Object>> dccInitConsumer = consumerArgumentCaptor.getValue();
        dccInitConsumer.accept(Collections.emptyMap());

        verifyNoMoreInteractions(botMessaging, ircBot, eventDispatcher);
    }

    @Test
    public void incoming_ctcp_query_passive_dcc() {
        String botNick = "Andy";
        // DCC SEND <filename> <ip> <port> <file size> <token>
        String incoming_message = "DCC SEND test1.bin 3232260964 0 6 1";
        DccCtcpQuery ctcpQuery = DccCtcpQuery.fromQueryString(incoming_message);

        startDccTransfer.handle(new StartDccTransferCommand(botNick, ctcpQuery, 3232260865L));

        verify(botMessaging).ask(eq(Address.FILENAME_RESOLVE), eq(new FilenameResolveRequest("test1.bin")), consumerArgumentCaptor.capture());
        Consumer<Map<String, Object>> resolvedFilenameConsumer = consumerArgumentCaptor.getValue();
        resolvedFilenameConsumer.accept(Collections.singletonMap("filename", "test1._x0x_.bin"));

        DccInitializeRequest query = DccInitializeRequest.builder()
                .bot("Andy")
                .filename("test1._x0x_.bin")
                .ip("192.168.99.100")
                .port(0)
                .passive(true)
                .size(6)
                .token(1)
                .build();
        verify(botMessaging).ask(eq(Address.DCC_INITIALIZE), eq(query), consumerArgumentCaptor.capture());
        Consumer<Map<String, Object>> dccInitConsumer = consumerArgumentCaptor.getValue();
        dccInitConsumer.accept(Collections.singletonMap("port", 12345));

        verify(ircBot).sendCtcpMessage("keex", "DCC SEND test1.bin 3232260865 12345 6 1");

        verifyNoMoreInteractions(botMessaging, ircBot, eventDispatcher);
    }

}