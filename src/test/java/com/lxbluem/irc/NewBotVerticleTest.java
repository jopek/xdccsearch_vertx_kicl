package com.lxbluem.irc;

import com.lxbluem.adapter.EventBusBotMessaging;
import com.lxbluem.domain.ports.BotMessaging;
import com.lxbluem.irc.adapter.InMemoryBotStateStorage;
import com.lxbluem.irc.adapter.InMemoryBotStorage;
import com.lxbluem.irc.usecase.BotFactory;
import com.lxbluem.irc.usecase.BotManagementService;
import com.lxbluem.irc.usecase.BotService;
import com.lxbluem.irc.usecase.ports.BotPort;
import com.lxbluem.irc.usecase.ports.BotStorage;
import com.lxbluem.irc.usecase.requestmodel.BotConnectionDetails;
import io.vertx.core.Handler;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.unit.Async;
import io.vertx.ext.unit.TestContext;
import io.vertx.ext.unit.junit.VertxUnitRunner;
import io.vertx.rxjava.core.Vertx;
import io.vertx.rxjava.core.eventbus.DeliveryContext;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;

import java.time.Clock;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

@RunWith(VertxUnitRunner.class)
public class NewBotVerticleTest {

    @Before
    public void setUp() {
    }

    @Test(timeout = 5000)
    public void async_behavior(TestContext context) {
        Vertx vertx = Vertx.vertx();

//        vertx.eventBus().addOutboundInterceptor(interceptor("out"));
//        vertx.eventBus().addInboundInterceptor(interceptor("in"));

        Clock clock = Clock.systemDefaultZone();
        BotMessaging botMessaging = new EventBusBotMessaging(vertx.eventBus(), clock);
        BotStorage botStorage = new InMemoryBotStorage();
        InMemoryBotStateStorage stateStorage = new InMemoryBotStateStorage();
        BotService botService = new BotService(botStorage, stateStorage, botMessaging, clock);
        BotPort botPortMock = mock(BotPort.class);
        BotFactory botFactory = ddd -> botPortMock;
        BotManagementService managementService = new BotManagementService(botStorage, botFactory, botService);

        NewBotVerticle verticle = new NewBotVerticle(botMessaging, managementService);
        vertx.deployVerticle(verticle);

        String address = "NewBotVerticle:POST:/v2/xfers";
        JsonObject jsonObject = new JsonObject()
                .put("method", "POST")
                .put("body", "{  \"name\": \"lala\",  \"nname\": \"local\",  \"naddr\": \"192.168.99.100\",  \"nport\": 6668,  \"cname\": \"#download\",  \"uname\": \"mybotDCCp\",  \"n\": 1}");

        AtomicReference<String> botname = new AtomicReference<>();
        vertx.eventBus()
                .request(address, jsonObject, context.asyncAssertSuccess(m -> {
                    System.out.println(m.body());
                    botname.set(((JsonObject) m.body()).getString("bot"));
                }));

        Async async3 = context.async();
        vertx.eventBus()
                .consumer("bot.init", m -> {
                    System.out.println(m.body());
                    async3.complete();
                });

        async3.await();


        ArgumentCaptor<BotConnectionDetails> connectionDetailsCaptor = ArgumentCaptor.forClass(BotConnectionDetails.class);
        verify(botPortMock).connect(connectionDetailsCaptor.capture());
        BotConnectionDetails connectionDetails = connectionDetailsCaptor.getValue();
        assertEquals(botname.get(), connectionDetails.getBotNick());
        assertEquals("name_" + botname.get(), connectionDetails.getName());
        assertEquals("realname_" + botname.get(), connectionDetails.getRealName());
        assertEquals("user_" + botname.get(), connectionDetails.getUser());
        assertEquals("192.168.99.100", connectionDetails.getServerHostName());
        assertEquals(6668, connectionDetails.getServerPort());

        ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);
        verify(botPortMock).joinChannel(captor.capture());
        assertEquals("#download", captor.getValue());
    }

    private Handler<DeliveryContext<Object>> interceptor(String direction) {
        return dc -> {
            Object body = dc.body();
            String address = dc.message().address();
            String sendPublish = dc.message().isSend() ? "send" : "publish";
            System.out.printf("%s INTERCEPTOR %s %s %s\n", direction, sendPublish, address, body);
            dc.next();
        };
    }
}