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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

@ExtendWith(MockitoExtension.class)
class JoinMoreChannelsNoticeMessageHandlerTest {

    private EventDispatcher eventDispatcher;
    private IrcBot ircBot;
    private AtomicInteger requestHookExecuted;
    private NoticeMessageHandler.SubHandler noticeMessageHandler;
    private State state;

    @Captor
    private ArgumentCaptor<Collection<String>> stringCollectionCaptor;

    @BeforeEach
    void setUp() {
        requestHookExecuted = new AtomicInteger();
        Clock clock = Clock.systemDefaultZone();
        ircBot = mock(IrcBot.class);
        BotStorage botStorage = new InMemoryBotStorage();
        StateStorage stateStorage = new InMemoryStateStorage();
        eventDispatcher = mock(EventDispatcher.class);
        noticeMessageHandler = new JoinMoreChannelsNoticeMessageHandler(botStorage, stateStorage);

        initialiseStorages(botStorage, stateStorage);
    }

    private void initialiseStorages(BotStorage botStorage, StateStorage stateStorage) {
        botStorage.save("Andy", ircBot);

        Pack pack = testPack();
        Runnable requestHook = () -> requestHookExecuted.addAndGet(1);
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
    void notice_message_handler_more_channels_required() {
        String botNick = "Andy";
        String remoteNick = "Zombie";
        String noticeMessage = "[#DOWNLOAD] \u00034!!!WARNING!!! YOU MUST IDLE IN #ZW-CHAT - IF YOU ATTEMPT TO DOWNLOAD WITHOUT BEING IN #ZW-CHAT YOU WILL BE BANNED!\n";
        Pack pack = testPack();

        state.channelReferences(pack.getChannelName(), Arrays.asList());

        noticeMessageHandler.handle(new NoticeMessageCommand(botNick, remoteNick, noticeMessage));

        verify(ircBot, never()).registerNickname(botNick);
        verify(ircBot).joinChannel(new HashSet<>(Collections.singletonList("#zw-chat")));

        verifyNoMoreInteractions(ircBot, eventDispatcher);
    }

    @Test
    void notice_message_handler_more_channels_required_after_request() {
        String botNick = "Andy";
        String remoteNick = "Zombie";
        String noticeMessage = "[#DOWNLOAD] \u00034!!!WARNING!!! YOU MUST IDLE IN #ZW-CHAT - IF YOU ATTEMPT TO DOWNLOAD WITHOUT BEING IN #ZW-CHAT YOU WILL BE BANNED!\n";
        Pack pack = testPack();

        state.channelReferences("#download", Arrays.asList("#someChannel"));
        state.channelNickList("#download", Collections.singletonList(pack.getNickName()));
        state.channelNickList("#someChannel", Arrays.asList("user1", "user2"));

        assertEquals(1, requestHookExecuted.get());

        assertTrue(state.isPackRequested());
        assertFalse(state.isRequestingPackPossible());

        noticeMessageHandler.handle(new NoticeMessageCommand(botNick, remoteNick, noticeMessage));

        verify(ircBot, never()).registerNickname(botNick);
        verify(ircBot).joinChannel(stringCollectionCaptor.capture());
        assertEquals(1, stringCollectionCaptor.getValue().size());
        assertTrue(stringCollectionCaptor.getValue().contains("#zw-chat"));

        assertFalse(state.isRequestingPackPossible());
        verifyNoMoreInteractions(ircBot, eventDispatcher);
    }

}
