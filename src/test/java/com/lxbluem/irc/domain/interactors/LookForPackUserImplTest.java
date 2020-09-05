package com.lxbluem.irc.domain.interactors;

import com.lxbluem.common.domain.Pack;
import com.lxbluem.common.domain.events.BotEvent;
import com.lxbluem.common.domain.events.BotExitedEvent;
import com.lxbluem.common.domain.events.BotFailedEvent;
import com.lxbluem.common.domain.ports.EventDispatcher;
import com.lxbluem.irc.adapters.InMemoryBotStateStorage;
import com.lxbluem.irc.adapters.InMemoryBotStorage;
import com.lxbluem.irc.domain.model.BotState;
import com.lxbluem.irc.domain.model.request.LookForPackUserCommand;
import com.lxbluem.irc.domain.ports.incoming.ExitBot;
import com.lxbluem.irc.domain.ports.incoming.LookForPackUser;
import com.lxbluem.irc.domain.ports.outgoing.BotStateStorage;
import com.lxbluem.irc.domain.ports.outgoing.BotStorage;
import com.lxbluem.irc.domain.ports.outgoing.IrcBot;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static java.util.Arrays.asList;
import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

public class LookForPackUserImplTest {
    private EventDispatcher eventDispatcher;
    private final Instant fixedInstant = Instant.parse("2020-08-10T10:11:22Z");
    private BotStateStorage stateStorage;
    private BotStorage botStorage;
    private LookForPackUser lookForPackUser;

    private final IrcBot ircBot = mock(IrcBot.class);
    private final AtomicInteger requestHookExecuted = new AtomicInteger();


    @Before
    public void setUp() {
        botStorage = new InMemoryBotStorage();
        stateStorage = new InMemoryBotStateStorage();
        Clock clock = Clock.fixed(fixedInstant, ZoneId.systemDefault());
        eventDispatcher = mock(EventDispatcher.class);
        ExitBot exitBot = new ExitBotImpl(botStorage, stateStorage, eventDispatcher, clock);
        initialiseStorages();
        lookForPackUser = new LookForPackUserImpl(stateStorage, exitBot, eventDispatcher, clock);
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
    public void users_in_channel() {
        List<String> usersInChannel = asList("operator", "keex", "doomsman", "hellbaby");
        LookForPackUserCommand command = new LookForPackUserCommand("Andy", "#download", usersInChannel);
        this.lookForPackUser.handle(command);

        verifyNoMoreInteractions(eventDispatcher, ircBot);

        assertTrue(stateStorage.get("Andy").isPresent());
        BotState botState = stateStorage.get("Andy").get();
        assertTrue(botState.isRemoteUserSeen());
    }

    @Test
    public void users_in_channel__remoteUser_of_target_channel_missing() {
        lookForPackUser.handle(new LookForPackUserCommand("Andy", "#download", asList("operator", "doomsman", "hellbaby")));

        ArgumentCaptor<BotEvent> messageSentCaptor = ArgumentCaptor.forClass(BotEvent.class);
        verify(eventDispatcher, times(2)).dispatch(messageSentCaptor.capture());
        List<BotEvent> eventList = messageSentCaptor.getAllValues();

        verify(ircBot).cancelDcc("keex");
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