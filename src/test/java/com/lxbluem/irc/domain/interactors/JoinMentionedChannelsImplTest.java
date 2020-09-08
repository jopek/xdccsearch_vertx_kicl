package com.lxbluem.irc.domain.interactors;

import com.lxbluem.common.domain.Pack;
import com.lxbluem.irc.adapters.InMemoryBotStorage;
import com.lxbluem.irc.adapters.InMemoryStateStorage;
import com.lxbluem.irc.domain.model.State;
import com.lxbluem.irc.domain.model.request.JoinMentionedChannelsCommand;
import com.lxbluem.irc.domain.ports.outgoing.BotStorage;
import com.lxbluem.irc.domain.ports.outgoing.IrcBot;
import com.lxbluem.irc.domain.ports.outgoing.StateStorage;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.runners.MockitoJUnitRunner;

import java.util.Arrays;
import java.util.Collection;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class JoinMentionedChannelsImplTest {

    private StateStorage stateStorage;
    private IrcBot ircBot;
    private BotStorage botStorage;

    @Captor
    private ArgumentCaptor<Collection<String>> stringCollectionCaptor;
    private final AtomicInteger requestHookExecuted = new AtomicInteger();
    private JoinMentionedChannelsImpl joinMentionedChannels;

    @Before
    public void setUp() {
        ircBot = mock(IrcBot.class);
        stateStorage = new InMemoryStateStorage();
        botStorage = new InMemoryBotStorage();
        initialiseStorages();
        requestHookExecuted.set(0);

        joinMentionedChannels = new JoinMentionedChannelsImpl(botStorage, stateStorage);
    }

    private void initialiseStorages() {
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
    public void channel_topic() {
        String topic = "join #room; for #help, otherwise [#voice] ";
        JoinMentionedChannelsCommand command = new JoinMentionedChannelsCommand("Andy", "#download", topic);
        joinMentionedChannels.handle(command);

        verify(ircBot).joinChannel(stringCollectionCaptor.capture());
        Collection<String> channelsToJoin = stringCollectionCaptor.getValue();
        assertEquals(2, channelsToJoin.size());
        assertTrue(channelsToJoin.containsAll(Arrays.asList("#voice", "#room")));
        verifyNoMoreInteractions(ircBot);
    }

    @Test
    public void channel_topic__no_other_channels_referenced() {
        String topic = "lalalal";
        JoinMentionedChannelsCommand command = new JoinMentionedChannelsCommand("Andy", "#download", topic);
        joinMentionedChannels.handle(command);

        verify(ircBot).joinChannel(stringCollectionCaptor.capture());
        assertTrue(stringCollectionCaptor.getValue().isEmpty());
        verifyNoMoreInteractions(ircBot);
    }


}