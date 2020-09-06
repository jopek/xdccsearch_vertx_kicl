package com.lxbluem.irc.domain.interactors.subhandlers;

import com.lxbluem.common.domain.Pack;
import com.lxbluem.common.domain.ports.EventDispatcher;
import com.lxbluem.irc.adapters.InMemoryBotStateStorage;
import com.lxbluem.irc.adapters.InMemoryBotStorage;
import com.lxbluem.irc.domain.model.BotState;
import com.lxbluem.irc.domain.model.request.NoticeMessageCommand;
import com.lxbluem.irc.domain.ports.incoming.NoticeMessageHandler;
import com.lxbluem.irc.domain.ports.outgoing.BotStateStorage;
import com.lxbluem.irc.domain.ports.outgoing.BotStorage;
import com.lxbluem.irc.domain.ports.outgoing.IrcBot;
import org.junit.Before;
import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.*;

public class NickNameRegisteredNoticeMessageHandlerTest {
    private EventDispatcher eventDispatcher;
    private IrcBot ircBot;
    private AtomicInteger requestHookExecuted;
    private NoticeMessageHandler.SubHandler noticeMessageHandler;
    private BotState botState;

    @Before
    public void setUp() {
        requestHookExecuted = new AtomicInteger();
        ircBot = mock(IrcBot.class);
        BotStorage botStorage = new InMemoryBotStorage();
        BotStateStorage stateStorage = new InMemoryBotStateStorage();
        eventDispatcher = mock(EventDispatcher.class);
        noticeMessageHandler = new NickNameRegisteredNoticeMessageHandler(botStorage, stateStorage);

        initialiseStorages(botStorage, stateStorage);
    }

    private void initialiseStorages(BotStorage botStorage, BotStateStorage stateStorage) {
        botStorage.save("Andy", ircBot);

        Pack pack = testPack();
        Runnable requestHook = () -> requestHookExecuted.incrementAndGet();
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

    @Test
    public void notice_message_handler_nickserv_registered_nick_request() {
        String botNick = "Andy";
        String remoteNick = "nickserv";
        String noticeMessage = "nickname Andy registered";
        Pack pack = testPack();

        botState.nickRegistryRequired();
        botState.channelNickList(pack.getChannelName(), Collections.singletonList(pack.getNickName()));
        botState.channelReferences(pack.getChannelName(), Arrays.asList());

        noticeMessageHandler.handle(new NoticeMessageCommand(botNick, remoteNick, noticeMessage));

        verify(ircBot, never()).registerNickname(botNick);
        verifyNoMoreInteractions(ircBot, eventDispatcher);
        assertEquals(1, requestHookExecuted.get());
        assertTrue(botState.isPackRequested());
    }


}