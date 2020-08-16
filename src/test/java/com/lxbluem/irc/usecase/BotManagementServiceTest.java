package com.lxbluem.irc.usecase;

import com.lxbluem.domain.Pack;
import com.lxbluem.irc.adapter.InMemoryBotStorage;
import com.lxbluem.irc.usecase.ports.BotPort;
import com.lxbluem.irc.usecase.ports.BotStorage;
import com.lxbluem.irc.usecase.requestmodel.BotConnectionDetails;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;

import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

public class BotManagementServiceTest {

    BotManagementService botManagementService;
    private final BotPort botPort = mock(BotPort.class);
    private final BotService botService = mock(BotService.class);
    private final BotStorage botStorage = new InMemoryBotStorage();

    @Before
    public void setUp() {
        BotFactory botFactory = botService -> botPort;

        this.botManagementService = new BotManagementService(
                botStorage,
                botFactory,
                botService);
    }

    @Test
    public void name() {
        Pack pack = Pack.builder()
                .nickName("keex")
                .networkName("nn")
                .serverHostName("192.168.99.100")
                .serverPort(6667)
                .channelName("#download")
                .build();
        String botNick = botManagementService.startTransferOf(pack);

        ArgumentCaptor<BotConnectionDetails> connectionDetailsCaptor = ArgumentCaptor.forClass(BotConnectionDetails.class);

        verify(botPort).connect(connectionDetailsCaptor.capture());
        BotConnectionDetails connectionDetails = connectionDetailsCaptor.getValue();
        assertEquals(pack.getServerHostName(), connectionDetails.getServerHostName());
        assertEquals(pack.getServerPort(), connectionDetails.getServerPort());

        verify(botPort).joinChannel(eq(pack.getChannelName()));
        verify(botService).init(eq(botNick), eq(pack));
    }
}