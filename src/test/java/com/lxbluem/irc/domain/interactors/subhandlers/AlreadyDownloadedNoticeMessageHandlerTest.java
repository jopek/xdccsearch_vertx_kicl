package com.lxbluem.irc.domain.interactors.subhandlers;

import com.lxbluem.common.adapter.EventbusEventDispatcher;
import com.lxbluem.common.domain.Pack;
import com.lxbluem.common.domain.events.BotNoticeEvent;
import com.lxbluem.irc.adapters.InMemoryBotStorage;
import com.lxbluem.irc.adapters.InMemoryStateStorage;
import com.lxbluem.irc.domain.model.State;
import com.lxbluem.irc.domain.model.request.NoticeMessageCommand;
import com.lxbluem.irc.domain.ports.outgoing.BotStorage;
import com.lxbluem.irc.domain.ports.outgoing.IrcBot;
import com.lxbluem.irc.domain.ports.outgoing.StateStorage;
import org.junit.Before;
import org.junit.Test;

import static org.mockito.Mockito.*;

public class AlreadyDownloadedNoticeMessageHandlerTest {

    private AlreadyDownloadedNoticeMessageHandler handler;
    private IrcBot ircBot;
    private ManualScheduledTaskExecution scheduledExecution;
    private EventbusEventDispatcher eventDispatcher;

    @Before
    public void setUp() {
        StateStorage stateStorage = new InMemoryStateStorage();
        BotStorage botStorage = new InMemoryBotStorage();
        scheduledExecution = new ManualScheduledTaskExecution();

        ircBot = mock(IrcBot.class);
        eventDispatcher = mock(EventbusEventDispatcher.class);

        handler = new AlreadyDownloadedNoticeMessageHandler(botStorage, stateStorage, eventDispatcher, scheduledExecution);

        initialiseStorages(botStorage, stateStorage);
    }

    private void initialiseStorages(BotStorage botStorage, StateStorage stateStorage) {
        botStorage.save("Andy", ircBot);

        Pack pack = testPack();
        State state = new State(pack, () -> {
        });
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
    public void no_request_if_timer_not_elapsed() {
        handler.handle(new NoticeMessageCommand("Andy", "", "you already requested that pack"));
        verify(ircBot, never()).requestDccPack("keex", 5);

        BotNoticeEvent expected = BotNoticeEvent.builder()
                .botNickName("Andy")
                .remoteNick("")
                .noticeMessage("you already requested that pack - retrying in 1min")
                .build();
        verify(eventDispatcher).dispatch(eq(expected));
    }

    @Test
    public void request_when_elapsed() {
        handler.handle(new NoticeMessageCommand("Andy", "", "you already requested that pack"));

        scheduledExecution.runTask();

        verify(ircBot).requestDccPack("keex", 5);

        verify(eventDispatcher).dispatch(eq(BotNoticeEvent.builder()
                .botNickName("Andy")
                .remoteNick("")
                .noticeMessage("you already requested that pack - retrying in 1min")
                .build()));

        verify(eventDispatcher).dispatch(eq(BotNoticeEvent.builder()
                .botNickName("Andy")
                .remoteNick("")
                .noticeMessage("retrying to request pack")
                .build()));
    }
}