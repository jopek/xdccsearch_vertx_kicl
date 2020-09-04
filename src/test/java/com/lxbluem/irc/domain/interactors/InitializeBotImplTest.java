package com.lxbluem.irc.domain.interactors;

import com.lxbluem.common.domain.Pack;
import com.lxbluem.common.domain.events.BotInitializedEvent;
import com.lxbluem.common.domain.events.Event;
import com.lxbluem.common.domain.ports.BotMessaging;
import com.lxbluem.common.domain.ports.EventDispatcher;
import com.lxbluem.irc.adapters.InMemoryBotStateStorage;
import com.lxbluem.irc.adapters.InMemoryBotStorage;
import com.lxbluem.irc.domain.BotService;
import com.lxbluem.irc.domain.model.request.BotConnectionDetails;
import com.lxbluem.irc.domain.model.request.InitializeBotCommand;
import com.lxbluem.irc.domain.ports.incoming.ExitBot;
import com.lxbluem.irc.domain.ports.incoming.InitializeBot;
import com.lxbluem.irc.domain.ports.outgoing.*;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.Assert.*;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.*;

public class InitializeBotImplTest {
    private BotStateStorage stateStorage;
    private BotMessaging botMessaging;
    private EventDispatcher eventDispatcher;
    private IrcBot ircBot;
    private BotStorage botStorage;
    private final Instant fixedInstant = Instant.parse("2020-08-10T10:11:22Z");

    private final NameGenerator nameGenerator = mock(NameGenerator.class);
    private InitializeBot initializeBot;

    @Before
    public void setUp() throws Exception {
        botMessaging = mock(BotMessaging.class);
        ircBot = mock(IrcBot.class);
        BotFactory botFactory = () -> ircBot;
        stateStorage = new InMemoryBotStateStorage();
        botStorage = new InMemoryBotStorage();
        Clock clock = Clock.fixed(fixedInstant, ZoneId.systemDefault());
        when(nameGenerator.getNick()).thenReturn("Andy");
        new AtomicInteger(0);
        eventDispatcher = mock(EventDispatcher.class);
        ExitBot exitBot = new ExitBotImpl(botStorage, stateStorage, eventDispatcher, clock);
        BotService botService = new BotService(
                botStorage,
                stateStorage,
                botMessaging,
                eventDispatcher,
                clock,
                nameGenerator,
                exitBot
        );

        initializeBot = new InitializeBotImpl(
                botStorage,
                stateStorage,
                eventDispatcher,
                clock,
                nameGenerator,
                botFactory,
                botService
        );
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
    public void initialize_service_for_bot() {
        assertFalse(stateStorage.get("Andy").isPresent());
        assertFalse(botStorage.get("Andy").isPresent());

        initializeBot.handle(new InitializeBotCommand(testPack()));

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

}