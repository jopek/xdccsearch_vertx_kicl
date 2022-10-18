package com.lxbluem.irc.domain.interactors;

import com.lxbluem.common.domain.Pack;
import com.lxbluem.common.domain.events.BotInitializedEvent;
import com.lxbluem.common.domain.events.Event;
import com.lxbluem.common.domain.ports.BotMessaging;
import com.lxbluem.common.domain.ports.EventDispatcher;
import com.lxbluem.irc.adapters.InMemoryBotStorage;
import com.lxbluem.irc.adapters.InMemoryStateStorage;
import com.lxbluem.irc.domain.model.request.BotConnectionDetails;
import com.lxbluem.irc.domain.model.request.InitializeBotCommand;
import com.lxbluem.irc.domain.ports.incoming.InitializeBot;
import com.lxbluem.irc.domain.ports.outgoing.BotFactory;
import com.lxbluem.irc.domain.ports.outgoing.BotStorage;
import com.lxbluem.irc.domain.ports.outgoing.IrcBot;
import com.lxbluem.irc.domain.ports.outgoing.NameGenerator;
import com.lxbluem.irc.domain.ports.outgoing.StateStorage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

class InitializeBotImplTest {
    private StateStorage stateStorage;
    private BotMessaging botMessaging;
    private EventDispatcher eventDispatcher;
    private IrcBot ircBot;
    private BotStorage botStorage;

    private final NameGenerator nameGenerator = mock(NameGenerator.class);
    private InitializeBot initializeBot;

    @BeforeEach
    void setUp() {
        botMessaging = mock(BotMessaging.class);
        ircBot = mock(IrcBot.class);
        BotFactory botFactory = () -> ircBot;
        stateStorage = new InMemoryStateStorage();
        botStorage = new InMemoryBotStorage();
        when(nameGenerator.getNick()).thenReturn("Andy");
        eventDispatcher = mock(EventDispatcher.class);

        initializeBot = new InitializeBotImpl(
                botStorage,
                stateStorage,
                eventDispatcher,
                nameGenerator,
                botFactory
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
    void initialize_service_for_bot() {
        assertFalse(stateStorage.get("Andy").isPresent());
        assertFalse(botStorage.get("Andy").isPresent());

        initializeBot.handle(new InitializeBotCommand(testPack()));

        ArgumentCaptor<Event> messageCaptor = ArgumentCaptor.forClass(Event.class);
        verify(eventDispatcher).dispatch(messageCaptor.capture());
        verify(ircBot).connect(any(BotConnectionDetails.class));
        verify(ircBot).joinChannel("#download");

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
