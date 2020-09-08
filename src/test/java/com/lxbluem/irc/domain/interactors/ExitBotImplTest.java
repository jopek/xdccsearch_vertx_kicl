package com.lxbluem.irc.domain.interactors;

import com.lxbluem.common.domain.Pack;
import com.lxbluem.common.domain.events.BotExitedEvent;
import com.lxbluem.common.domain.ports.BotMessaging;
import com.lxbluem.common.domain.ports.EventDispatcher;
import com.lxbluem.irc.adapters.InMemoryBotStateStorage;
import com.lxbluem.irc.adapters.InMemoryBotStorage;
import com.lxbluem.irc.domain.exception.BotNotFoundException;
import com.lxbluem.irc.domain.model.BotState;
import com.lxbluem.irc.domain.model.request.DccFinishedExitCommand;
import com.lxbluem.irc.domain.model.request.ReasonedExitCommand;
import com.lxbluem.irc.domain.model.request.RequestedExitCommand;
import com.lxbluem.irc.domain.ports.incoming.ExitBot;
import com.lxbluem.irc.domain.ports.outgoing.BotStateStorage;
import com.lxbluem.irc.domain.ports.outgoing.BotStorage;
import com.lxbluem.irc.domain.ports.outgoing.IrcBot;
import com.lxbluem.irc.domain.ports.outgoing.NameGenerator;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.Arrays;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.*;

public class ExitBotImplTest {

    private BotStateStorage stateStorage;
    private BotMessaging botMessaging;
    private EventDispatcher eventDispatcher;
    private IrcBot ircBot;
    private BotStorage botStorage;
    private final Instant fixedInstant = Instant.parse("2020-08-10T10:11:22Z");

    private final NameGenerator nameGenerator = mock(NameGenerator.class);
    private final AtomicInteger requestHookExecuted = new AtomicInteger();
    private ExitBot exitBot;
    private BotState botState;


    @Before
    public void setUp() throws Exception {
        botMessaging = mock(BotMessaging.class);
        ircBot = mock(IrcBot.class);
        stateStorage = new InMemoryBotStateStorage();
        botStorage = new InMemoryBotStorage();
        when(nameGenerator.getNick()).thenReturn("Andy");
        eventDispatcher = mock(EventDispatcher.class);
        initializeStorages(botStorage, stateStorage);
        requestHookExecuted.set(0);
        Clock clock = Clock.fixed(fixedInstant, ZoneId.systemDefault());

        exitBot = new ExitBotImpl(botStorage, stateStorage, eventDispatcher, clock);
    }

    @Test
    public void exit_before_executing() {
        botState.channelReferences("#download", Arrays.asList());

        exitBot.handle(new RequestedExitCommand("Andy"));

        verify(ircBot, never()).cancelDcc("keex");
        verify(ircBot).terminate();
        verify(eventDispatcher, times(1)).dispatch(any(BotExitedEvent.class));
        verifyZeroInteractions(ircBot, botMessaging, eventDispatcher);

        botState.channelNickList("#download", Arrays.asList("keex", "user2", "user3"));

        assertEquals(1, requestHookExecuted.get());
    }

    @Test
    public void terminte_bot_manually_with_running_tranfer() {
        botState.dccTransferRunning();
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
        assertEquals(fixedInstant.toEpochMilli(), sentMesssage.getTimestamp());
        assertFalse(stateStorage.get("Andy").isPresent());
        assertEquals("Andy", sentMesssage.getBot());
        assertEquals(fixedInstant.toEpochMilli(), sentMesssage.getTimestamp());
    }

    @Test
    public void terminte_bot_finished_dcc_with_running_tranfer() {
        botState.dccTransferRunning();
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
        assertEquals(fixedInstant.toEpochMilli(), sentMesssage.getTimestamp());
        assertFalse(stateStorage.get("Andy").isPresent());
        assertEquals("Andy", sentMesssage.getBot());
        assertEquals(fixedInstant.toEpochMilli(), sentMesssage.getTimestamp());
    }

    @Test
    public void terminte_bot_manually_without_running_tranfer() {
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
        assertEquals(fixedInstant.toEpochMilli(), sentMesssage.getTimestamp());
        assertFalse(stateStorage.get("Andy").isPresent());
        assertEquals("Andy", sentMesssage.getBot());
        assertEquals(fixedInstant.toEpochMilli(), sentMesssage.getTimestamp());
    }

    @Test(expected = BotNotFoundException.class)
    public void terminate_bot_manually_for_missing_bot() {
        exitBot.handle(new RequestedExitCommand("nonexistent"));
        verify(ircBot).terminate();

        verifyNoMoreInteractions(botMessaging, ircBot, eventDispatcher);
    }

    @Test
    public void terminate_bot() {
        botState.dccTransferRunning();
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
        assertEquals(fixedInstant.toEpochMilli(), sentMesssage.getTimestamp());
    }

    private void initializeStorages(BotStorage botStorage, BotStateStorage stateStorage) {
        botStorage.save("Andy", ircBot);

        Pack pack = testPack();
        Runnable requestHook = () -> requestHookExecuted.addAndGet(1);
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

}