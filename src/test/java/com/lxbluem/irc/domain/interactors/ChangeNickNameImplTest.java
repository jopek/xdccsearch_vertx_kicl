package com.lxbluem.irc.domain.interactors;

import com.lxbluem.common.domain.events.BotRenamedEvent;
import com.lxbluem.common.domain.ports.EventDispatcher;
import com.lxbluem.irc.adapters.InMemoryBotStorage;
import com.lxbluem.irc.domain.model.request.ChangeNickNameCommand;
import com.lxbluem.irc.domain.ports.incoming.ChangeNickName;
import com.lxbluem.irc.domain.ports.outgoing.BotStorage;
import com.lxbluem.irc.domain.ports.outgoing.IrcBot;
import com.lxbluem.irc.domain.ports.outgoing.NameGenerator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

class ChangeNickNameImplTest {

    private IrcBot ircBot;
    private BotStorage botStorage;
    private EventDispatcher eventDispatcher;
    private final Instant fixedInstant = Instant.parse("2020-08-10T10:11:22Z");
    private ChangeNickName changeNickName;
    private NameGenerator nameGenerator;

    @BeforeEach
    void setUp() {
        nameGenerator = mock(NameGenerator.class);
        eventDispatcher = mock(EventDispatcher.class);
        botStorage = new InMemoryBotStorage();
        ircBot = mock(IrcBot.class);
        botStorage.save("Andy", ircBot);
        Clock clock = Clock.fixed(fixedInstant, ZoneId.systemDefault());

        changeNickName = new ChangeNickNameImpl(botStorage, nameGenerator, eventDispatcher);
    }

    @Test
    void register_new_nick_when_rejected() {
        when(nameGenerator.getNick()).thenReturn("Randy");

        ChangeNickNameCommand command = new ChangeNickNameCommand("Andy", "something happened; serverMessages; more serverMessages");
        changeNickName.handle(command);

        verify(ircBot).changeNickname("Randy");

        // VERIFY this assumption
//        assertFalse(botStorage.get("Andy").isPresent());
//        assertTrue(botStorage.get("Randy").isPresent());

        ArgumentCaptor<BotRenamedEvent> messageSentCaptor = ArgumentCaptor.forClass(BotRenamedEvent.class);
        verify(eventDispatcher).dispatch(messageSentCaptor.capture());

        BotRenamedEvent sentMesssage = messageSentCaptor.getValue();
        assertEquals("Andy", sentMesssage.getBot());
        assertEquals("something happened; serverMessages; more serverMessages", sentMesssage.getMessage());
        assertEquals("Randy", sentMesssage.getRenameto());

        verifyNoMoreInteractions(ircBot, eventDispatcher);
    }

}
