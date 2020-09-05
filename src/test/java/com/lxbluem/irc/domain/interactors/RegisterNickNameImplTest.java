package com.lxbluem.irc.domain.interactors;

import com.lxbluem.irc.adapters.InMemoryBotStorage;
import com.lxbluem.irc.domain.model.request.RegisterNickNameCommand;
import com.lxbluem.irc.domain.ports.incoming.RegisterNickName;
import com.lxbluem.irc.domain.ports.outgoing.BotStorage;
import com.lxbluem.irc.domain.ports.outgoing.IrcBot;
import org.junit.Before;
import org.junit.Test;

import static org.mockito.Mockito.*;

public class RegisterNickNameImplTest {

    private IrcBot ircBot;
    private BotStorage botStorage;
    private RegisterNickName registerNickName;


    @Before
    public void setUp() throws Exception {
        ircBot = mock(IrcBot.class);
        botStorage = new InMemoryBotStorage();
        botStorage.save("Andy", ircBot);
        registerNickName = new RegisterNickNameImpl(botStorage);
    }

    @Test
    public void message_of_the_day() {
        registerNickName.handle(new RegisterNickNameCommand("Andy"));

        verify(ircBot).registerNickname("Andy");
        verifyNoMoreInteractions(ircBot);
    }

}