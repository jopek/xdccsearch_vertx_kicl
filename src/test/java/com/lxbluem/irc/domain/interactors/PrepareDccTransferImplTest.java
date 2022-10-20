package com.lxbluem.irc.domain.interactors;

import com.lxbluem.common.domain.Pack;
import com.lxbluem.common.domain.ports.BotMessaging;
import com.lxbluem.common.domain.ports.EventDispatcher;
import com.lxbluem.common.infrastructure.Address;
import com.lxbluem.irc.adapters.InMemoryBotStorage;
import com.lxbluem.irc.adapters.InMemoryStateStorage;
import com.lxbluem.irc.domain.model.State;
import com.lxbluem.irc.domain.model.request.CtcpDccSend;
import com.lxbluem.irc.domain.model.request.DccInitializeRequest;
import com.lxbluem.irc.domain.model.request.DccResumeAcceptTransferCommand;
import com.lxbluem.irc.domain.model.request.DccSendTransferCommand;
import com.lxbluem.irc.domain.model.request.FilenameResolveRequest;
import com.lxbluem.irc.domain.ports.incoming.ExitBot;
import com.lxbluem.irc.domain.ports.incoming.PrepareDccTransfer;
import com.lxbluem.irc.domain.ports.outgoing.BotStorage;
import com.lxbluem.irc.domain.ports.outgoing.IrcBot;
import com.lxbluem.irc.domain.ports.outgoing.NameGenerator;
import com.lxbluem.irc.domain.ports.outgoing.StateStorage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.Serializable;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PrepareDccTransferImplTest {
    private BotMessaging botMessaging;
    private EventDispatcher eventDispatcher;
    private IrcBot ircBot;

    @Captor
    private ArgumentCaptor<Consumer<Map<String, Object>>> consumerArgumentCaptor;

    private final AtomicInteger requestHookExecuted = new AtomicInteger();
    private PrepareDccTransfer prepareDccTransfer;
    private State state;

    @BeforeEach
    void setUp() {
        botMessaging = mock(BotMessaging.class);
        ircBot = mock(IrcBot.class);
        eventDispatcher = mock(EventDispatcher.class);

        StateStorage stateStorage = new InMemoryStateStorage();
        BotStorage botStorage = new InMemoryBotStorage();

        initializeStorages(botStorage, stateStorage);
        requestHookExecuted.set(0);

        ExitBot exitBot = new ExitBotImpl(botStorage, stateStorage, eventDispatcher);
        prepareDccTransfer = new PrepareDccTransferImpl(botStorage, stateStorage, botMessaging, exitBot);
    }

    private void initializeStorages(BotStorage botStorage, StateStorage stateStorage) {
        botStorage.save("Andy", ircBot);

        Pack pack = testPack();
        Runnable requestHook = requestHookExecuted::incrementAndGet;
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
                .packName("test1.bin")
                .build();
    }

    @Test
    void incoming_invalid_ctcp_query() {
        String botNick = "Andy";
        String incoming_message = "crrrrrap";
        CtcpDccSend ctcpQuery = CtcpDccSend.fromQueryString(incoming_message);

        prepareDccTransfer.handle(new DccSendTransferCommand(botNick, ctcpQuery, 0L));

        verifyNoMoreInteractions(botMessaging, ircBot, eventDispatcher);
    }

    @Test
    void incoming_ctcp_query_requests_search_for_expected_filename() {
        state.remoteSendsCorrectPack();

        String botNick = "Andy";
        // DCC SEND <filename> <ip> <port> <file size>
        String incoming_message = "DCC SEND expected_filename.txt 3232260964 50000 6";
        CtcpDccSend ctcpQuery = CtcpDccSend.fromQueryString(incoming_message);
        DccSendTransferCommand command = new DccSendTransferCommand(botNick, ctcpQuery, 0L);

        prepareDccTransfer.handle(command);

        verify(botMessaging).ask(eq(Address.FILENAME_RESOLVE), any(Serializable.class), any(Consumer.class));
        verifyNoMoreInteractions(botMessaging, ircBot, eventDispatcher);
    }

    @Test
    void incoming_ctcp_query_requests_search_for_unexpected_filename() {
        String botNick = "Andy";
        // DCC SEND <filename> <ip> <port> <file size>
        String incoming_message = "DCC SEND unexpected_filename.txt 3232260964 50000 6";
        CtcpDccSend ctcpQuery = CtcpDccSend.fromQueryString(incoming_message);
        DccSendTransferCommand command = new DccSendTransferCommand(botNick, ctcpQuery, 0L);

        prepareDccTransfer.handle(command);

        verifyNoMoreInteractions(botMessaging, ircBot, eventDispatcher);
    }

    @Test
    void incoming_ctcp_query_active_dcc() {
        state.remoteSendsCorrectPack();
        String botNick = "Andy";
        // DCC SEND <filename> <ip> <port> <file size>
        String incoming_message = "DCC SEND test1.bin 3232260964 50000 6";
        CtcpDccSend ctcpQuery = CtcpDccSend.fromQueryString(incoming_message);

        prepareDccTransfer.handle(new DccSendTransferCommand(botNick, ctcpQuery, 3232260964L));

        verify(botMessaging).ask(eq(Address.FILENAME_RESOLVE), eq(new FilenameResolveRequest("test1.bin", 6L)), consumerArgumentCaptor.capture());
        Consumer<Map<String, Object>> resolvedFilenameConsumer = consumerArgumentCaptor.getValue();
        Map<String, Object> resolveAnswer = new HashMap<>();
        resolveAnswer.put("filename", "test1._x0x_.bin");
        resolveAnswer.put("position", 6L);
        resolveAnswer.put("isComplete", false);
        resolvedFilenameConsumer.accept(resolveAnswer);

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
    void incoming_ctcp_query_passive_dcc() {
        state.remoteSendsCorrectPack();
        String botNick = "Andy";
        // DCC SEND <filename> <ip> <port> <file size> <token>
        String incoming_message = "DCC SEND test1.bin 3232260964 0 6 1";
        CtcpDccSend ctcpQuery = CtcpDccSend.fromQueryString(incoming_message);

        prepareDccTransfer.handle(new DccSendTransferCommand(botNick, ctcpQuery, 3232260865L));

        verify(botMessaging).ask(eq(Address.FILENAME_RESOLVE), eq(new FilenameResolveRequest("test1.bin", 6L)), consumerArgumentCaptor.capture());
        Consumer<Map<String, Object>> resolvedFilenameConsumer = consumerArgumentCaptor.getValue();
        Map<String, Object> resolveAnswer = new HashMap<>();
        resolveAnswer.put("filename", "test1._x0x_.bin");
        resolveAnswer.put("position", 6L);
        resolveAnswer.put("isComplete", false);
        resolvedFilenameConsumer.accept(resolveAnswer);

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

    @Test
    void incoming_ctcp_query_active_dcc__resume() {
        state.remoteSendsCorrectPack();
        String botNick = "Andy";
        // DCC SEND <filename> <ip> <port> <file size>
        String incoming_message = "DCC SEND test1.bin 3232260964 50000 6000";
        CtcpDccSend ctcpQuery = CtcpDccSend.fromQueryString(incoming_message);

        prepareDccTransfer.handle(new DccSendTransferCommand(botNick, ctcpQuery, 0L));

        verify(botMessaging).ask(eq(Address.FILENAME_RESOLVE), eq(new FilenameResolveRequest("test1.bin", 6000L)), consumerArgumentCaptor.capture());
        Consumer<Map<String, Object>> resolvedFilenameConsumer = consumerArgumentCaptor.getValue();
        Map<String, Object> resolveAnswer = new HashMap<>();
        resolveAnswer.put("filename", "test1._x0x_.bin.part");
        resolveAnswer.put("position", 3000L);
        resolveAnswer.put("isComplete", false);
        resolvedFilenameConsumer.accept(resolveAnswer);

        //DCC RESUME <filename> <port from dcc send> <resume position>
        verify(ircBot).sendCtcpMessage("keex", "DCC RESUME test1.bin 50000 3000");

        verifyNoMoreInteractions(botMessaging, ircBot, eventDispatcher);

        assertEquals("test1._x0x_.bin.part", state.getResolvedFilename());
    }

    @Test
    void incoming_ctcp_query_active_dcc__resume_accepted() {
        state.resolvedFilename("test1._x0x_.bin");
        state.resolvedFilePartialSize(3000L);
        String botNick = "Andy";
        // > DCC SEND <filename> <ip> <port> <file size>
        state.initialDccSendQuery(CtcpDccSend.fromQueryString("DCC SEND test1.bin 3232260964 50000 6000"));
        // > DCC ACCEPT <filename> 0 <position> <token>]
        String incoming_accept_message = "DCC ACCEPT test1.bin 50000 3000";

        prepareDccTransfer.handle(new DccResumeAcceptTransferCommand(botNick, 0L));

        DccInitializeRequest dccInitializeRequest = DccInitializeRequest.builder()
                .bot("Andy")
                .filename("test1._x0x_.bin")
                .ip("192.168.99.100")
                .port(50000)
                .passive(false)
                .size(6000L)
                .continueFromPosition(3000L)
                .build();
        verify(botMessaging).ask(eq(Address.DCC_INITIALIZE), eq(dccInitializeRequest), consumerArgumentCaptor.capture());
        Consumer<Map<String, Object>> dccInitConsumer = consumerArgumentCaptor.getValue();
        dccInitConsumer.accept(Collections.emptyMap());

        verifyNoMoreInteractions(botMessaging, ircBot, eventDispatcher);
    }

    @Test
    void incoming_ctcp_query_passive_dcc__resume() {
        state.remoteSendsCorrectPack();
        String botNick = "Andy";
        // DCC SEND <filename> <ip> <port> <file size>
        String incoming_message = "DCC SEND test1.bin 3232260964 0 6000 111";
        CtcpDccSend ctcpQuery = CtcpDccSend.fromQueryString(incoming_message);

        prepareDccTransfer.handle(new DccSendTransferCommand(botNick, ctcpQuery, 0L));

        verify(botMessaging).ask(eq(Address.FILENAME_RESOLVE), eq(new FilenameResolveRequest("test1.bin", 6000L)), consumerArgumentCaptor.capture());
        Consumer<Map<String, Object>> resolvedFilenameConsumer = consumerArgumentCaptor.getValue();
        Map<String, Object> resolveAnswer = new HashMap<>();
        resolveAnswer.put("filename", "test1._x0x_.bin.part");
        resolveAnswer.put("position", 3000L);
        resolveAnswer.put("isComplete", false);
        resolvedFilenameConsumer.accept(resolveAnswer);

        //DCC RESUME <filename> <port from dcc send> <resume position>
        verify(ircBot).sendCtcpMessage("keex", "DCC RESUME test1.bin 0 3000 111");

        verifyNoMoreInteractions(botMessaging, ircBot, eventDispatcher);

        assertEquals("test1._x0x_.bin.part", state.getResolvedFilename());
    }

    @Test
    void incoming_ctcp_query_passive_dcc__resume_accepted() {
        state.resolvedFilename("test1._x0x_.bin");
        state.resolvedFilePartialSize(3000L);
        String botNick = "Andy";
        // > DCC SEND <filename> <ip> <port> <file size> <token>
        state.initialDccSendQuery(CtcpDccSend.fromQueryString("DCC SEND test1.bin 3232260964 0 6000 111"));

        prepareDccTransfer.handle(new DccResumeAcceptTransferCommand(botNick, 16843009L));

        DccInitializeRequest dccInitializeRequest = DccInitializeRequest.builder()
                .bot("Andy")
                .filename("test1._x0x_.bin")
                .ip("192.168.99.100")
                .port(0)
                .token(111)
                .passive(true)
                .size(6000L)
                .continueFromPosition(3000L)
                .build();
        verify(botMessaging).ask(eq(Address.DCC_INITIALIZE), eq(dccInitializeRequest), consumerArgumentCaptor.capture());
        Consumer<Map<String, Object>> dccInitConsumer = consumerArgumentCaptor.getValue();
        dccInitConsumer.accept(Collections.singletonMap("port", 12312));

        verify(ircBot).sendCtcpMessage("keex", "DCC SEND test1.bin 16843009 12312 6000 111");
        verifyNoMoreInteractions(botMessaging, ircBot, eventDispatcher);
    }
}
