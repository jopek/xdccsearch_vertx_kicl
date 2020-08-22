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

    private Vertx vertx;
    private BotPort mockBot;
    private final String startAddress = "NewBotVerticle:POST:/v2/xfers";
    private final String stopAddress = "NewBotVerticle:DELETE:/v2/xfers/:botname";
    private final JsonObject startMessage = new JsonObject()
            .put("method", "POST")
            .put("body", "{  \"name\": \"lala\",  \"nname\": \"local\",  \"naddr\": \"192.168.99.100\",  \"nport\": 6668,  \"cname\": \"#download\",  \"uname\": \"mybotDCCp\",  \"n\": 1}");

    @Before
    public void setUp() {
        vertx = Vertx.vertx();

//        vertx.eventBus().addOutboundInterceptor(interceptor("out"));
//        vertx.eventBus().addInboundInterceptor(interceptor("in"));

        Clock clock = Clock.systemDefaultZone();
        BotMessaging botMessaging = new EventBusBotMessaging(vertx.eventBus(), clock);
        BotStorage botStorage = new InMemoryBotStorage();
        InMemoryBotStateStorage stateStorage = new InMemoryBotStateStorage();
        mockBot = mock(BotPort.class);
        BotFactory botFactory = ddd -> mockBot;
        NameGenerator nameGenerator = () -> "Andy";
        BotService botService = new BotService(botStorage, stateStorage, botMessaging, botFactory, clock, nameGenerator);
        NewBotVerticle verticle = new NewBotVerticle(botService);
        vertx.deployVerticle(verticle);
    }

    @Test(timeout = 5000)
    public void startTransfer(TestContext context) {

        AtomicReference<String> botname = new AtomicReference<>();
        vertx.eventBus()
                .request(startAddress, startMessage, context.asyncAssertSuccess(m -> {
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
        verify(mockBot).connect(connectionDetailsCaptor.capture());
        BotConnectionDetails connectionDetails = connectionDetailsCaptor.getValue();
        assertEquals(botname.get(), connectionDetails.getBotNick());
        assertEquals("name_" + botname.get(), connectionDetails.getName());
        assertEquals("realname_" + botname.get(), connectionDetails.getRealName());
        assertEquals("user_" + botname.get(), connectionDetails.getUser());
        assertEquals("192.168.99.100", connectionDetails.getServerHostName());
        assertEquals(6668, connectionDetails.getServerPort());

        ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);
        verify(mockBot).joinChannel(captor.capture());
        assertEquals("#download", captor.getValue());
    }

    @Test(timeout = 2000)
    public void stopTransfer_fails_when_bot_missing(TestContext context) {
        JsonObject stopMessage = new JsonObject()
                .put("method", "DELETE")
                .put("params", new JsonObject().put("botname", "missing"));

        vertx.eventBus()
                .request(stopAddress, stopMessage, context.asyncAssertFailure(
                        reply -> context.assertEquals("bot 'missing' not found", reply.getMessage())
                ));
    }

    @Test(timeout = 5000)
    public void stopTransfer(TestContext context) {

        AtomicReference<String> botname = new AtomicReference<>();
        Async botnameAsync = context.async();
        vertx.eventBus()
                .request(startAddress, startMessage, context.asyncAssertSuccess(m -> {
                    String botnameFromReply = ((JsonObject) m.body()).getString("bot");
                    botname.set(botnameFromReply);
                    botnameAsync.complete();
                }));
        botnameAsync.await();

        System.out.printf("got botname %s\n", botname.get());

        JsonObject stopMessage = new JsonObject()
                .put("method", "DELETE")
                .put("params", new JsonObject().put("botname", botname.get()));
        System.out.println(stopMessage.encode());

        Async async = context.async();
        vertx.eventBus()
                .consumer(Address.BOT_DCC_TERMINATE.getAddressValue(), result -> {
                    JsonObject body = (JsonObject) result.body();
                    System.out.println(body.encode());
                    String botnameFromMessage = body.getString("bot");
                    context.assertEquals(botname.get(), botnameFromMessage);
                    async.complete();
                });

        vertx.eventBus()
                .request(stopAddress, stopMessage, context.asyncAssertSuccess(reply -> {
                    JsonObject body = (JsonObject) reply.body();
                    String botnameFromReply = body.getString("bot");
                    context.assertEquals(botname.get(), botnameFromReply);
                    System.out.println(body.encode());
                }));

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