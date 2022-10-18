package com.lxbluem.irc.domain.interactors;

import com.lxbluem.common.domain.Pack;
import com.lxbluem.common.domain.events.BotExitedEvent;
import com.lxbluem.common.domain.ports.BotMessaging;
import com.lxbluem.common.domain.ports.EventDispatcher;
import com.lxbluem.irc.adapters.InMemoryBotStorage;
import com.lxbluem.irc.adapters.InMemoryStateStorage;
import com.lxbluem.irc.domain.exception.BotNotFoundException;
import com.lxbluem.irc.domain.model.State;
import com.lxbluem.irc.domain.model.request.DccFinishedExitCommand;
import com.lxbluem.irc.domain.model.request.ReasonedExitCommand;
import com.lxbluem.irc.domain.model.request.RequestedExitCommand;
import com.lxbluem.irc.domain.ports.incoming.ExitBot;
import com.lxbluem.irc.domain.ports.outgoing.BotStorage;
import com.lxbluem.irc.domain.ports.outgoing.IrcBot;
import com.lxbluem.irc.domain.ports.outgoing.NameGenerator;
import com.lxbluem.irc.domain.ports.outgoing.StateStorage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.Arrays;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

class ExitBotImplTest {

    private StateStorage stateStorage;
    private BotMessaging botMessaging;
    private EventDispatcher eventDispatcher;
    private IrcBot ircBot;
    private BotStorage botStorage;
    private final Instant fixedInstant = Instant.parse("2020-08-10T10:11:22Z");

    private final NameGenerator nameGenerator = mock(NameGenerator.class);
    private final AtomicInteger requestHookExecuted = new AtomicInteger();
    private ExitBot exitBot;
    private State state;


    @BeforeEach
    void setUp() throws Exception {
        botMessaging = mock(BotMessaging.class);
        ircBot = mock(IrcBot.class);
        stateStorage = new InMemoryStateStorage();
        botStorage = new InMemoryBotStorage();
        when(nameGenerator.getNick()).thenReturn("Andy");
        eventDispatcher = mock(EventDispatcher.class);
        initializeStorages(botStorage, stateStorage);
        requestHookExecuted.set(0);
        Clock clock = Clock.fixed(fixedInstant, ZoneId.systemDefault());

        exitBot = new ExitBotImpl(botStorage, stateStorage, eventDispatcher);
    }

    @Test
    void exit_before_executing() {
        state.channelReferences("#download", Arrays.asList());

        exitBot.handle(new RequestedExitCommand("Andy"));

        verify(ircBot, never()).cancelDcc("keex");
        verify(ircBot).terminate();
        verify(eventDispatcher, times(1)).dispatch(any(BotExitedEvent.class));
        verifyNoInteractions(ircBot, botMessaging, eventDispatcher);

        state.channelNickList("#download", Arrays.asList("keex", "user2", "user3"));

        assertEquals(1, requestHookExecuted.get());
    }

    @Test
    void terminte_bot_manually_with_running_tranfer() {
        state.dccTransferRunning();
        exitBot.handle(new RequestedExitCommand("Andy"));

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
        assertFalse(stateStorage.get("Andy").isPresent());
        assertEquals("Andy", sentMesssage.getBot());
    }

    @Test
    void terminte_bot_finished_dcc_with_running_tranfer() {
        state.dccTransferRunning();
        exitBot.handle(new DccFinishedExitCommand("Andy", "transfer finished"));

        verify(ircBot, never()).cancelDcc("keex");
        verify(ircBot).terminate();

        assertFalse(botStorage.get("Andy").isPresent());
        assertFalse(stateStorage.get("Andy").isPresent());

        ArgumentCaptor<BotExitedEvent> messageSentCaptor = ArgumentCaptor.forClass(BotExitedEvent.class);
        verify(eventDispatcher).dispatch(messageSentCaptor.capture());

        verifyNoMoreInteractions(botMessaging, ircBot, eventDispatcher);

        BotExitedEvent sentMesssage = messageSentCaptor.getValue();
        assertEquals("Andy", sentMesssage.getBot());
        assertEquals("Bot Andy exiting because transfer finished", sentMesssage.getMessage());
        assertFalse(stateStorage.get("Andy").isPresent());
        assertEquals("Andy", sentMesssage.getBot());
    }

    @Test
    void terminte_bot_manually_without_running_tranfer() {
        exitBot.handle(new RequestedExitCommand("Andy"));

        verify(ircBot, never()).cancelDcc("keex");
        verify(ircBot).terminate();

        assertFalse(botStorage.get("Andy").isPresent());
        assertFalse(stateStorage.get("Andy").isPresent());

        ArgumentCaptor<BotExitedEvent> messageSentCaptor = ArgumentCaptor.forClass(BotExitedEvent.class);
        verify(eventDispatcher).dispatch(messageSentCaptor.capture());

        verifyNoMoreInteractions(botMessaging, ircBot, eventDispatcher);

        BotExitedEvent sentMesssage = messageSentCaptor.getValue();
        assertEquals("Andy", sentMesssage.getBot());
        assertEquals("Bot Andy exiting because requested shutdown", sentMesssage.getMessage());
        assertFalse(stateStorage.get("Andy").isPresent());
        assertEquals("Andy", sentMesssage.getBot());
    }

    @Test
    void terminate_bot_manually_for_missing_bot() {
        RequestedExitCommand nonexistent = new RequestedExitCommand("nonexistent");
        assertThrows(BotNotFoundException.class, () -> exitBot.handle(nonexistent));
        verify(ircBot).terminate();

        verifyNoMoreInteractions(botMessaging, ircBot, eventDispatcher);
    }

    @Test
    void terminate_bot() {
        state.dccTransferRunning();
        exitBot.handle(new ReasonedExitCommand("Andy", "failure"));
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
    }

    private void initializeStorages(BotStorage botStorage, StateStorage stateStorage) {
        botStorage.save("Andy", ircBot);

        Pack pack = testPack();
        Runnable requestHook = () -> requestHookExecuted.addAndGet(1);
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
                .build();
    }

}
