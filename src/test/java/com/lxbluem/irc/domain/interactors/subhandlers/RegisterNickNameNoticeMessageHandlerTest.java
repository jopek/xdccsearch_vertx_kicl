package com.lxbluem.irc.domain.interactors.subhandlers;

import com.lxbluem.common.domain.Pack;
import com.lxbluem.common.domain.ports.EventDispatcher;
import com.lxbluem.irc.adapters.InMemoryBotStorage;
import com.lxbluem.irc.adapters.InMemoryStateStorage;
import com.lxbluem.irc.domain.model.State;
import com.lxbluem.irc.domain.model.request.NoticeMessageCommand;
import com.lxbluem.irc.domain.ports.incoming.NoticeMessageHandler;
import com.lxbluem.irc.domain.ports.outgoing.BotStorage;
import com.lxbluem.irc.domain.ports.outgoing.IrcBot;
import com.lxbluem.irc.domain.ports.outgoing.StateStorage;
import org.junit.Before;
import org.junit.Test;

import java.util.concurrent.atomic.AtomicInteger;

import static org.mockito.Mockito.*;

public class RegisterNickNameNoticeMessageHandlerTest {
    private EventDispatcher eventDispatcher;
    private IrcBot ircBot;
    private AtomicInteger requestHookExecuted;
    private NoticeMessageHandler.SubHandler noticeMessageHandler;
    private State state;

    @Before
    public void setUp() {
        requestHookExecuted = new AtomicInteger();
        ircBot = mock(IrcBot.class);
        BotStorage botStorage = new InMemoryBotStorage();
        StateStorage stateStorage = new InMemoryStateStorage();
        eventDispatcher = mock(EventDispatcher.class);
        noticeMessageHandler = new RegisterNickNameNoticeMessageHandler(botStorage, stateStorage);

        initialiseStorages(botStorage, stateStorage);
    }

    private void initialiseStorages(BotStorage botStorage, StateStorage stateStorage) {
        botStorage.save("Andy", ircBot);

        Pack pack = testPack();
        Runnable requestHook = () -> requestHookExecuted.incrementAndGet();
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

    @Test
    public void notice_message_handler_nickserv_register_nick() {
        String botNick = "Andy";
        String remoteNick = "nickserv";
        String noticeMessage = "your nickname is not registered. to register it, use";

        noticeMessageHandler.handle(new NoticeMessageCommand(botNick, remoteNick, noticeMessage));

        verify(ircBot).registerNickname(botNick);
        verifyNoMoreInteractions(ircBot, eventDispatcher);
    }


}