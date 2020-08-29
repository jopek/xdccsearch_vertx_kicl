package com.lxbluem.irc;

import com.lxbluem.common.adapter.EventBusBotMessaging;
import com.lxbluem.common.adapter.EventbusEventDispatcher;
import com.lxbluem.common.domain.events.DccFailedEvent;
import com.lxbluem.common.domain.events.DccFinishedEvent;
import com.lxbluem.common.domain.ports.BotMessaging;
import com.lxbluem.common.domain.ports.EventDispatcher;
import com.lxbluem.common.infrastructure.Address;
import com.lxbluem.irc.adapters.InMemoryBotStateStorage;
import com.lxbluem.irc.adapters.InMemoryBotStorage;
import com.lxbluem.irc.domain.BotService;
import com.lxbluem.irc.domain.model.request.BotConnectionDetails;
import com.lxbluem.irc.domain.ports.*;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.eventbus.DeliveryContext;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.unit.Async;
import io.vertx.ext.unit.TestContext;
import io.vertx.ext.unit.junit.VertxUnitRunner;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;

import java.time.Clock;
import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.Set;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

@RunWith(VertxUnitRunner.class)
public class NewBotVerticleTest {

    private Vertx vertx;
    private IrcBot mockBot;
    private final String startAddress = "NewBotVerticle:POST:/xfers";
    private final String stopAddress = "NewBotVerticle:DELETE:/xfers/:botname";
    private final JsonObject startMessage = new JsonObject()
            .put("method", "POST")
            .put("body", "{  \"name\": \"lala\",  \"nname\": \"local\",  \"naddr\": \"192.168.99.100\",  \"nport\": 6668,  \"cname\": \"#download\",  \"uname\": \"mybotDCCp\",  \"n\": 1}");

    @Before
    public void setUp(TestContext context) {
        vertx = Vertx.vertx();

        vertx.eventBus().addOutboundInterceptor(interceptor("out"));
//        vertx.eventBus().addInboundInterceptor(interceptor("in"));

        String botNickName = "Andy";
        NameGenerator nameGenerator = () -> botNickName;

        Clock clock = Clock.systemDefaultZone();
        BotMessaging botMessaging = new EventBusBotMessaging(vertx.eventBus(), clock);
        BotStorage botStorage = new InMemoryBotStorage();
        DccBotStateStorage stateStorage = new InMemoryBotStateStorage();
        mockBot = mock(IrcBot.class);
        BotFactory botFactory = ignored -> mockBot;
        EventDispatcher eventDispatcher = new EventbusEventDispatcher(vertx.eventBus());
        BotService botService = new BotService(botStorage, stateStorage, botMessaging, /*eventDispatcher,*/ eventDispatcher, botFactory, clock, nameGenerator);
        NewBotVerticle verticle = new NewBotVerticle(botService);
        vertx.deployVerticle(verticle, context.asyncAssertSuccess());
    }

    @Test(timeout = 3_000)
    public void startTransfer_bot_connects_to_irc(TestContext context) {
        vertx.eventBus()
                .<JsonObject>request(startAddress, startMessage, context.asyncAssertSuccess(m -> {
                    context.assertEquals("Andy", m.body().getString("bot"));
                }));

        Async botConnect = context.async();
        vertx.eventBus()
                .consumer("bot.init", m -> {
                    botConnect.complete();
                });
        botConnect.await();

        ArgumentCaptor<BotConnectionDetails> connectionDetailsCaptor = ArgumentCaptor.forClass(BotConnectionDetails.class);
        verify(mockBot).connect(connectionDetailsCaptor.capture());

        BotConnectionDetails connectionDetails = connectionDetailsCaptor.getValue();
        assertEquals("Andy", connectionDetails.getBotNick());
        assertEquals("name_Andy", connectionDetails.getName());
        assertEquals("realname_Andy", connectionDetails.getRealName());
        assertEquals("user_Andy", connectionDetails.getUser());
        assertEquals("192.168.99.100", connectionDetails.getServerHostName());
        assertEquals(6668, connectionDetails.getServerPort());

        ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);
        verify(mockBot).joinChannel(captor.capture());
        assertEquals("#download", captor.getValue());
    }

    @Test(timeout = 3_000)
    public void stopTransfer_fails_when_bot_missing(TestContext context) {
        JsonObject stopMessage = new JsonObject()
                .put("method", "DELETE")
                .put("params", new JsonObject().put("botname", "missing"));

        vertx.eventBus()
                .request(stopAddress, stopMessage, context.asyncAssertFailure(
                        reply -> context.assertEquals("bot 'missing' not found", reply.getMessage())
                ));
    }

    @Test(timeout = 3_000)
    public void stopTransfer_via_request(TestContext context) {
        vertx.eventBus()
                .<JsonObject>request(startAddress, startMessage, context.asyncAssertSuccess(m -> {
                    context.assertEquals("Andy", m.body().getString("bot"));
                }));

        Async async2 = context.async();
        vertx.eventBus()
                .<JsonObject>consumer(Address.BOT_EXITED.address(), result -> {
                    context.assertEquals("Andy", result.body().getString("bot"));
                    List<String> expectedMessageKeys = Arrays.asList("bot", "timestamp");
                    Set<String> messageKeys = result.body().getMap().keySet();
                    context.assertTrue(messageKeys.containsAll(expectedMessageKeys));
                    async2.complete();
                });

        // trigger
        JsonObject stopMessage = new JsonObject()
                .put("method", "DELETE")
                .put("params", new JsonObject().put("botname", "Andy"));

        System.out.printf("serialized https request: %s\n", stopMessage.encode());
        vertx.eventBus()
                .<JsonObject>request(stopAddress, stopMessage, context.asyncAssertSuccess(reply -> {
                    JsonObject body = reply.body();
                    context.assertEquals("Andy", body.getString("bot"));
                    System.out.printf("stop request reply: %s\n", body.encode());
                }));
    }

    @Test(timeout = 3_000)
    public void stopTransfer_because_dcc_transfer_finished(TestContext context) {
        vertx.eventBus()
                .<JsonObject>request(startAddress, startMessage, context.asyncAssertSuccess(m -> {
                    context.assertEquals("Andy", m.body().getString("bot"));
                }));

        Async async = context.async();
        vertx.eventBus()
                .consumer(Address.BOT_EXITED.address(), result -> {
                    JsonObject body = (JsonObject) result.body();
                    System.out.println(body.encode());
                    context.assertEquals("Andy", body.getString("bot"));
                    context.assertEquals("Bot Andy exiting because DCC transfer finished", body.getString("message"));
                    async.complete();
                });

        DccFinishedEvent dccFinishedEvent = new DccFinishedEvent("Andy", Instant.now().toEpochMilli());
        JsonObject dccFinishJsonObject = JsonObject.mapFrom(dccFinishedEvent);
        vertx.eventBus()
                .publish(Address.DCC_FINISHED.address(), dccFinishJsonObject);

    }

    @Test(timeout = 3_000)
    public void stopTransfer_because_dcc_transfer_failed(TestContext context) {
        vertx.eventBus()
                .<JsonObject>request(startAddress, startMessage, context.asyncAssertSuccess(m -> {
                    context.assertEquals("Andy", m.body().getString("bot"));
                }));

        Async exitAsync = context.async();
        vertx.eventBus()
                .consumer(Address.BOT_EXITED.address(), result -> {
                    JsonObject body = (JsonObject) result.body();
                    System.out.println(body.encode());
                    context.assertEquals("Andy", body.getString("bot"));
                    context.assertEquals("Bot Andy exiting because no space on filesystem", body.getString("message"));
                    exitAsync.complete();
                });

        DccFailedEvent dccFailedEvent = new DccFailedEvent("Andy", Instant.now().toEpochMilli(), "no space on filesystem");
        JsonObject dccFinishJsonObject = JsonObject.mapFrom(dccFailedEvent);
        vertx.eventBus()
                .publish(Address.DCC_FAILED.address(), dccFinishJsonObject);

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