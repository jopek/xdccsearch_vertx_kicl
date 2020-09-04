package com.lxbluem.irc.domain;

import com.lxbluem.common.domain.Pack;
import com.lxbluem.common.domain.events.BotEvent;
import com.lxbluem.common.domain.events.BotExitedEvent;
import com.lxbluem.common.domain.events.BotFailedEvent;
import com.lxbluem.common.domain.events.BotRenamedEvent;
import com.lxbluem.common.domain.ports.BotMessaging;
import com.lxbluem.common.domain.ports.EventDispatcher;
import com.lxbluem.irc.adapters.InMemoryBotStateStorage;
import com.lxbluem.irc.adapters.InMemoryBotStorage;
import com.lxbluem.irc.domain.interactors.ExitBotImpl;
import com.lxbluem.irc.domain.interactors.NoticeMessageHandlerImpl;
import com.lxbluem.irc.domain.model.BotState;
import com.lxbluem.irc.domain.ports.incoming.ExitBot;
import com.lxbluem.irc.domain.ports.incoming.NoticeMessageHandler;
import com.lxbluem.irc.domain.ports.outgoing.BotStateStorage;
import com.lxbluem.irc.domain.ports.outgoing.BotStorage;
import com.lxbluem.irc.domain.ports.outgoing.IrcBot;
import com.lxbluem.irc.domain.ports.outgoing.NameGenerator;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.runners.MockitoJUnitRunner;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

import static java.util.Arrays.asList;
import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class BotServiceTest {

    private BotStateStorage stateStorage;
    private BotService botService;
    private BotMessaging botMessaging;
    private EventDispatcher eventDispatcher;
    private IrcBot ircBot;
    private BotStorage botStorage;
    private final Instant fixedInstant = Instant.parse("2020-08-10T10:11:22Z");

    @Captor
    private ArgumentCaptor<Collection<String>> stringCollectionCaptor;
    @Captor
    private ArgumentCaptor<Consumer<Map<String, Object>>> consumerArgumentCaptor;
    private final NameGenerator nameGenerator = mock(NameGenerator.class);
    private final AtomicInteger requestHookExecuted = new AtomicInteger();
    private ExitBot exitBot;


    @Before
    public void setUp() {
        botMessaging = mock(BotMessaging.class);
        ircBot = mock(IrcBot.class);
        stateStorage = new InMemoryBotStateStorage();
        botStorage = new InMemoryBotStorage();
        Clock clock = Clock.fixed(fixedInstant, ZoneId.systemDefault());
        when(nameGenerator.getNick()).thenReturn("Andy");
        eventDispatcher = mock(EventDispatcher.class);
        exitBot = new ExitBotImpl(botStorage, stateStorage, eventDispatcher, clock);
        initialiseStorages();
        requestHookExecuted.set(0);

        NoticeMessageHandler noticeMessageHanlder = new NoticeMessageHandlerImpl(botStorage, stateStorage, eventDispatcher, clock, exitBot);
        botService = new BotService(
                botStorage,
                stateStorage,
                botMessaging,
                eventDispatcher,
                clock,
                nameGenerator,
                exitBot,
                noticeMessageHanlder
        );
    }

    private void initialiseStorages() {
        botStorage.save("Andy", ircBot);

        Pack pack = testPack();
        Runnable requestHook = () -> requestHookExecuted.addAndGet(1);
        stateStorage.save("Andy", new BotState(pack, requestHook));
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
    public void users_in_channel() {
        botService.usersInChannel("Andy", "#download", asList("operator", "keex", "doomsman", "hellbaby"));

        verifyNoMoreInteractions(botMessaging, ircBot);

        assertEquals(1, requestHookExecuted.get());
        assertTrue(stateStorage.get("Andy").isPresent());
        BotState botState = stateStorage.get("Andy").get();
        assertTrue(botState.isRemoteUserSeen());
    }

    @Test
    public void users_in_channel__remoteUser_of_target_channel_missing() {
        botService.usersInChannel("Andy", "#download", asList("operator", "doomsman", "hellbaby"));

        ArgumentCaptor<BotEvent> messageSentCaptor = ArgumentCaptor.forClass(BotEvent.class);
        verify(eventDispatcher, times(2)).dispatch(messageSentCaptor.capture());
        List<BotEvent> eventList = messageSentCaptor.getAllValues();

        verify(ircBot).cancelDcc("keex");
        verify(ircBot).terminate();
        verifyNoMoreInteractions(botMessaging, ircBot, eventDispatcher);

        BotFailedEvent failedEvent = (BotFailedEvent) eventList.get(0);
        assertEquals("bot keex not in channel #download", failedEvent.getMessage());
        BotExitedEvent exitedEvent = (BotExitedEvent) eventList.get(1);
        assertEquals("Bot Andy exiting because bot keex not in channel #download", exitedEvent.getMessage());
        assertFalse(stateStorage.get("Andy").isPresent());
        assertFalse(botStorage.get("Andy").isPresent());
    }

    @Test
    public void channel_topic() {
        botService.channelTopic("Andy", "#download", "join #room; for #help, otherwise [#voice] ");

        verify(ircBot).joinChannel(stringCollectionCaptor.capture());
        Collection<String> channelsToJoin = stringCollectionCaptor.getValue();
        assertEquals(2, channelsToJoin.size());
        assertTrue(channelsToJoin.containsAll(Arrays.asList("#voice", "#room")));
        verifyNoMoreInteractions(botMessaging, ircBot, eventDispatcher);
    }

    @Test
    public void channel_topic__no_other_channels_referenced() {
        botService.channelTopic("Andy", "#download", "lalalal");

        verify(ircBot).joinChannel(stringCollectionCaptor.capture());
        assertTrue(stringCollectionCaptor.getValue().isEmpty());
        verifyNoMoreInteractions(botMessaging, ircBot, eventDispatcher);
    }

    @Test
    public void message_of_the_day() {
        botService.messageOfTheDay("Andy", asList("message of the", "dayyyyyy", "in multiple strings"));

        verify(ircBot).registerNickname("Andy");
        verifyNoMoreInteractions(botMessaging, ircBot, eventDispatcher);
    }

    @Test
    public void register_new_nick_when_rejected() {
        when(nameGenerator.getNick()).thenReturn("Randy");
        botService.changeNick("Andy", "something happened; serverMessages; more serverMessages");

        verify(ircBot).changeNickname(eq("Randy"));

        ArgumentCaptor<BotRenamedEvent> messageSentCaptor = ArgumentCaptor.forClass(BotRenamedEvent.class);
        verify(eventDispatcher).dispatch(messageSentCaptor.capture());

        BotRenamedEvent sentMesssage = messageSentCaptor.getValue();
        assertEquals("Andy", sentMesssage.getBot());
        assertEquals("something happened; serverMessages; more serverMessages", sentMesssage.getMessage());
        assertEquals("Randy", sentMesssage.getRenameto());
        assertEquals(fixedInstant.toEpochMilli(), sentMesssage.getTimestamp());

        verifyNoMoreInteractions(botMessaging, ircBot, eventDispatcher);
    }

    @Test
    public void channel_requires_account_registry__account_register_nick() {
        BotState botState = stateStorage.get("Andy").get();
        String botNick = "Andy";
        String numericCommandMessage = "You need to be identified to a registered account to join this channel";
        botState.channelReferences("#download", Arrays.asList("#2", "#mg-lounge"));
        botState.channelNickList("#download", Arrays.asList("keex"));
        botState.channelReferences("#2", Collections.emptyList());

        botService.channelRequiresAccountRegistry(botNick, "#mg-lounge", numericCommandMessage);

        assertEquals(1, requestHookExecuted.get());
        verifyNoMoreInteractions(botMessaging, ircBot, eventDispatcher);
    }

}