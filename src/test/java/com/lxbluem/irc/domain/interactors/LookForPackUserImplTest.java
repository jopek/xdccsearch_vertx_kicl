package com.lxbluem.irc.domain.interactors;

import com.lxbluem.common.domain.Pack;
import com.lxbluem.common.domain.events.BotEvent;
import com.lxbluem.common.domain.events.BotExitedEvent;
import com.lxbluem.common.domain.events.BotFailedEvent;
import com.lxbluem.common.domain.ports.EventDispatcher;
import com.lxbluem.irc.adapters.InMemoryBotStorage;
import com.lxbluem.irc.adapters.InMemoryStateStorage;
import com.lxbluem.irc.domain.model.State;
import com.lxbluem.irc.domain.model.request.LookForPackUserCommand;
import com.lxbluem.irc.domain.ports.incoming.ExitBot;
import com.lxbluem.irc.domain.ports.incoming.LookForPackUser;
import com.lxbluem.irc.domain.ports.outgoing.BotStorage;
import com.lxbluem.irc.domain.ports.outgoing.IrcBot;
import com.lxbluem.irc.domain.ports.outgoing.StateStorage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static java.util.Arrays.asList;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

class LookForPackUserImplTest {
    private EventDispatcher eventDispatcher;
    private final Instant fixedInstant = Instant.parse("2020-08-10T10:11:22Z");
    private StateStorage stateStorage;
    private BotStorage botStorage;
    private LookForPackUser lookForPackUser;

    private final IrcBot ircBot = mock(IrcBot.class);
    private final AtomicInteger requestHookExecuted = new AtomicInteger();


    @BeforeEach
    void setUp() {
        botStorage = new InMemoryBotStorage();
        stateStorage = new InMemoryStateStorage();
        Clock clock = Clock.fixed(fixedInstant, ZoneId.systemDefault());
        eventDispatcher = mock(EventDispatcher.class);
        ExitBot exitBot = new ExitBotImpl(botStorage, stateStorage, eventDispatcher);
        initialiseStorages();
        lookForPackUser = new LookForPackUserImpl(stateStorage, exitBot, eventDispatcher);
    }

    private void initialiseStorages() {
        botStorage.save("Andy", ircBot);

        Pack pack = testPack();
        Runnable requestHook = () -> requestHookExecuted.addAndGet(1);
        stateStorage.save("Andy", new State(pack, requestHook));
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
    void users_in_channel() {
        List<String> usersInChannel = asList("operator", "keex", "doomsman", "hellbaby");
        LookForPackUserCommand command = new LookForPackUserCommand("Andy", "#download", usersInChannel);
        this.lookForPackUser.handle(command);

        verifyNoMoreInteractions(eventDispatcher, ircBot);

        assertTrue(stateStorage.get("Andy").isPresent());
        State state = stateStorage.get("Andy").get();
        assertTrue(state.isRemoteUserSeen());
    }

    @Test
    void users_in_channel__remoteUser_of_target_channel_missing() {
        lookForPackUser.handle(new LookForPackUserCommand("Andy", "#download", asList("operator", "doomsman", "hellbaby")));

        ArgumentCaptor<BotEvent> messageSentCaptor = ArgumentCaptor.forClass(BotEvent.class);
        verify(eventDispatcher, times(2)).dispatch(messageSentCaptor.capture());
        List<BotEvent> eventList = messageSentCaptor.getAllValues();

        verify(ircBot, never()).cancelDcc("keex");
        verify(ircBot).terminate();
        verifyNoMoreInteractions(ircBot, eventDispatcher);

        BotFailedEvent failedEvent = (BotFailedEvent) eventList.get(0);
        assertEquals("bot keex not in channel #download", failedEvent.getMessage());
        BotExitedEvent exitedEvent = (BotExitedEvent) eventList.get(1);
        assertEquals("Bot Andy exiting because bot keex not in channel #download", exitedEvent.getMessage());
        assertFalse(stateStorage.get("Andy").isPresent());
        assertFalse(botStorage.get("Andy").isPresent());
    }


}
