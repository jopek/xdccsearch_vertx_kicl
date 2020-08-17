package com.lxbluem.irc.usecase;

import com.lxbluem.domain.Pack;
import com.lxbluem.domain.ports.BotMessaging;
import com.lxbluem.irc.adapter.InMemoryBotStateStorage;
import com.lxbluem.irc.adapter.InMemoryBotStorage;
import com.lxbluem.irc.domain.DccBotState;
import com.lxbluem.irc.domain.DefaultDccBotState;
import com.lxbluem.irc.usecase.ports.BotPort;
import com.lxbluem.irc.usecase.ports.BotStorage;
import com.lxbluem.irc.usecase.ports.DccBotStateStorage;
import com.lxbluem.irc.usecase.requestmodel.BotNoticeMessage;
import com.lxbluem.irc.usecase.requestmodel.BotRenameMessage;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static java.util.Arrays.asList;
import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

public class BotServiceTest {

    private DccBotStateStorage stateStorage;
    private BotService botService;
    private BotMessaging botMessaging;

    private final Instant fixedInstant = Instant.parse("2020-08-10T10:11:22Z");
    private BotPort botPort;

    @Before
    public void setUp() {
        stateStorage = new InMemoryBotStateStorage();
        botMessaging = mock(BotMessaging.class);
        botPort = mock(BotPort.class);
        BotStorage botStorage = new InMemoryBotStorage();
        botStorage.save("Andy", botPort);

        Clock clock = Clock.fixed(fixedInstant, ZoneId.systemDefault());
        botService = new BotService(botStorage, stateStorage, botMessaging, clock);
    }

    @Test
    public void initialize_service_for_bot() {
        assertNull(stateStorage.getBotStateByNick("Andy"));

        botService.init("Andy", testPack());

        assertNotNull(stateStorage.getBotStateByNick("Andy"));
        Assert.assertEquals(testPack(), stateStorage.getBotStateByNick("Andy").getPack());
    }

    @Test
    public void mark_channel_joined() {
        botService.init("Andy", testPack());
        botService.onRequestedChannelJoinComplete("Andy", "#download");

        DccBotState dccBotState = stateStorage.getBotStateByNick("Andy");
        Set<String> joinedChannels = ((DefaultDccBotState) dccBotState).getJoinedChannels();
        List<String> expectedJoinedChannels = Collections.singletonList("#download");
        assertTrue(joinedChannels.containsAll(expectedJoinedChannels));
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
        botService.init("Andy", testPack());
        botService.usersInChannel("Andy", "#download", asList("operator", "keex", "doomsman", "hellbaby"));

        DccBotState dccBotState = stateStorage.getBotStateByNick("Andy");
        assertTrue(((DefaultDccBotState) dccBotState).isRemoteUserSeen());
    }

    @Test
    public void channel_topic() {
        botService.init("Andy", testPack());
        botService.channelTopic("Andy", "#download", "join #room; for #help, otherwise [#voice] ");

        HashSet<String> channelReferences = new HashSet<>(asList("#room", "#voice"));
        DccBotState dccBotState = stateStorage.getBotStateByNick("Andy");
        assertEquals(channelReferences, ((DefaultDccBotState) dccBotState).getReferencedChannelNames());
    }

    @Test
    public void message_of_the_day() {
        botService.messageOfTheDay("Andy", asList("message of the", "dayyyyyy", "in multiple strings"));

        verify(botPort).registerNickname("Andy");
    }

    @Test
    public void register_new_nick_when_rejected() {
        botService.changeNick("Andy", "something happened; serverMessages; more serverMessages");

        ArgumentCaptor<String> nickChangeCaptor = ArgumentCaptor.forClass(String.class);
        verify(botPort).changeNickname(nickChangeCaptor.capture());

        ArgumentCaptor<BotRenameMessage> messageSentCaptor = ArgumentCaptor.forClass(BotRenameMessage.class);
        verify(botMessaging).notify(messageSentCaptor.capture());

        String actual = nickChangeCaptor.getValue();
        assertNotEquals("Andy", actual);
        assertNotEquals("", actual);
        assertEquals(4, actual.length());

        BotRenameMessage sentMesssage = messageSentCaptor.getValue();
        assertEquals("Andy", sentMesssage.getBot());
        assertEquals("something happened; serverMessages; more serverMessages", sentMesssage.getMessage());
        assertNotEquals("Andy", sentMesssage.getNewBotName());
        assertEquals(fixedInstant.toEpochMilli(), sentMesssage.getTimestamp());
    }

    @Test
    public void notice_message_handler_nickserv_register_nick() {
        String botNick = "Andy";

        String remoteNick = "nickserv";
        String noticeMessage = "your nickname is not registered. to register it, use";

        botService.init(botNick, testPack());
        botService.handleNoticeMessage(botNick, remoteNick, noticeMessage);

        verify(botPort).registerNickname(botNick);

    }

    @Test
    public void notice_message_handler_nickserv_registered_nick_request() {
        String botNick = "Andy";
        String remoteNick = "nickserv";
        String noticeMessage = "nickname Andy registered";

        Pack pack = testPack();

        botService.init(botNick, pack);
        DccBotState botState = stateStorage.getBotStateByNick(botNick);
        botState.nickRegistryRequired();
        botState.channelNickList(pack.getChannelName(), Collections.singletonList(pack.getNickName()));
        botState.joinedChannel(pack.getChannelName());

        botService.handleNoticeMessage(botNick, remoteNick, noticeMessage);

        verify(botPort, never()).registerNickname(botNick);
        verify(botPort).requestDccPack("keex", 5);

        ArgumentCaptor<BotNoticeMessage> messageSentCaptor = ArgumentCaptor.forClass(BotNoticeMessage.class);
        verify(botMessaging).notify(messageSentCaptor.capture());

        BotNoticeMessage sentMesssage = messageSentCaptor.getValue();
        assertEquals("Andy", sentMesssage.getBot());
        assertEquals("requesting pack #5 from keex", sentMesssage.getMessage());
        assertEquals(fixedInstant.toEpochMilli(), sentMesssage.getTimestamp());
    }

}