package com.lxbluem.irc.domain.interactors;

import com.lxbluem.irc.adapters.InMemoryBotStorage;
import com.lxbluem.irc.domain.model.request.RegisterNickNameCommand;
import com.lxbluem.irc.domain.ports.incoming.RegisterNickName;
import com.lxbluem.irc.domain.ports.outgoing.BotStorage;
import com.lxbluem.irc.domain.ports.outgoing.IrcBot;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

class RegisterNickNameImplTest {

    private IrcBot ircBot;
    private BotStorage botStorage;
    private RegisterNickName registerNickName;


    @BeforeEach
    void setUp() throws Exception {
        ircBot = mock(IrcBot.class);
        botStorage = new InMemoryBotStorage();
        botStorage.save("Andy", ircBot);
        registerNickName = new RegisterNickNameImpl(botStorage);
    }

    @Test
    void message_of_the_day() {
        registerNickName.handle(new RegisterNickNameCommand("Andy"));

        verify(ircBot).registerNickname("Andy");
        verifyNoMoreInteractions(ircBot);
    }

}
