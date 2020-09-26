package com.lxbluem.irc.domain.interactors.subhandlers;

import com.lxbluem.common.domain.Pack;
import com.lxbluem.common.domain.events.BotEvent;
import com.lxbluem.common.domain.events.BotExitedEvent;
import com.lxbluem.common.domain.events.BotFailedEvent;
import com.lxbluem.common.domain.ports.EventDispatcher;
import com.lxbluem.irc.adapters.InMemoryBotStorage;
import com.lxbluem.irc.adapters.InMemoryStateStorage;
import com.lxbluem.irc.domain.interactors.ExitBotImpl;
import com.lxbluem.irc.domain.model.State;
import com.lxbluem.irc.domain.model.request.NoticeMessageCommand;
import com.lxbluem.irc.domain.ports.incoming.ExitBot;
import com.lxbluem.irc.domain.ports.incoming.NoticeMessageHandler;
import com.lxbluem.irc.domain.ports.outgoing.BotStorage;
import com.lxbluem.irc.domain.ports.outgoing.IrcBot;
import com.lxbluem.irc.domain.ports.outgoing.StateStorage;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.mockito.Mockito.*;

public class FailureNoticeMessageHandlerTest {

    private NoticeMessageHandler.SubHandler noticeMessageHandler;
    private EventDispatcher eventDispatcher;
    private IrcBot ircBot;
    private AtomicInteger requestHookExecuted;


    @Before
    public void setUp() {
        requestHookExecuted = new AtomicInteger();
        ircBot = mock(IrcBot.class);
        BotStorage botStorage = new InMemoryBotStorage();
        StateStorage stateStorage = new InMemoryStateStorage();
        eventDispatcher = mock(EventDispatcher.class);
        ExitBot exitBot = new ExitBotImpl(botStorage, stateStorage, eventDispatcher);
        noticeMessageHandler = new FailureNoticeMessageHandler(exitBot, eventDispatcher);

        initialiseStorages(botStorage, stateStorage);
    }

    private void initialiseStorages(BotStorage botStorage, StateStorage stateStorage) {
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
    public void notice_message_handler_closing_connection_by_user() {
        String botNick = "Andy";
        String remoteNick = "keex";
        String noticeMessage = "closing connection: transfer canceled by user";

        boolean messageHandled = noticeMessageHandler.handle(new NoticeMessageCommand(botNick, remoteNick, noticeMessage));

        verifyNoMoreInteractions(ircBot, eventDispatcher);
        assertFalse(messageHandled);
    }

    @Test
    public void notice_message_handler_connection_refused() {
        String botNick = "Andy";
        String remoteNick = "keex";
        String noticeMessage = "connection refused";

        noticeMessageHandler.handle(new NoticeMessageCommand(botNick, remoteNick, noticeMessage));

        ArgumentCaptor<BotEvent> messageSentCaptor = ArgumentCaptor.forClass(BotEvent.class);
        verify(eventDispatcher, times(2)).dispatch(messageSentCaptor.capture());
        List<BotEvent> eventList = messageSentCaptor.getAllValues();

        BotFailedEvent failMessage = (BotFailedEvent) eventList.get(0);
        assertEquals("Andy", failMessage.getBot());
        assertEquals("connection refused", failMessage.getMessage());

        BotExitedEvent exitMessage = (BotExitedEvent) eventList.get(1);
        assertEquals("Andy", exitMessage.getBot());
        assertEquals("Bot Andy exiting because connection refused", exitMessage.getMessage());

        verify(ircBot, never()).cancelDcc("keex");
        verify(ircBot).terminate();
        verifyNoMoreInteractions(ircBot, eventDispatcher);
    }

}