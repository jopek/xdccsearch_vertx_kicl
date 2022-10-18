package com.lxbluem.irc;

import com.lxbluem.common.adapter.EventbusEventDispatcher;
import com.lxbluem.common.domain.events.DccFailedEvent;
import com.lxbluem.common.domain.events.DccFinishedEvent;
import com.lxbluem.common.domain.events.DccStartedEvent;
import com.lxbluem.common.domain.ports.EventDispatcher;
import com.lxbluem.common.infrastructure.Address;
import com.lxbluem.irc.adapters.InMemoryBotStorage;
import com.lxbluem.irc.adapters.InMemoryStateStorage;
import com.lxbluem.irc.domain.interactors.ExitBotImpl;
import com.lxbluem.irc.domain.interactors.InitializeBotImpl;
import com.lxbluem.irc.domain.interactors.ToggleDccTransferStartedImpl;
import com.lxbluem.irc.domain.model.request.BotConnectionDetails;
import com.lxbluem.irc.domain.ports.incoming.ExitBot;
import com.lxbluem.irc.domain.ports.incoming.InitializeBot;
import com.lxbluem.irc.domain.ports.incoming.ToggleDccTransferStarted;
import com.lxbluem.irc.domain.ports.outgoing.BotFactory;
import com.lxbluem.irc.domain.ports.outgoing.BotStorage;
import com.lxbluem.irc.domain.ports.outgoing.IrcBot;
import com.lxbluem.irc.domain.ports.outgoing.NameGenerator;
import com.lxbluem.irc.domain.ports.outgoing.StateStorage;
import io.vertx.core.Handler;
import io.vertx.core.Verticle;
import io.vertx.core.Vertx;
import io.vertx.core.eventbus.DeliveryContext;
import io.vertx.core.json.JsonObject;
import io.vertx.junit5.VertxExtension;
import io.vertx.reactivex.ext.unit.Async;
import io.vertx.reactivex.ext.unit.TestContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;

import java.time.Clock;
import java.util.Arrays;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

@ExtendWith(VertxExtension.class)
class NewBotVerticleTest {

    private Vertx vertx;
    private IrcBot mockBot;
    private final String startAddress = "NewBotVerticle:POST:/xfers";
    private final String stopAddress = "NewBotVerticle:DELETE:/xfers/:botname";
    private final JsonObject startMessage = new JsonObject()
            .put("method", "POST")
            .put("body", "{  \"name\": \"lala\",  \"nname\": \"local\",  \"naddr\": \"192.168.99.100\",  \"nport\": 6668,  \"cname\": \"#download\",  \"uname\": \"remoteBotName\",  \"n\": 1}");

    @BeforeEach
    void setUp(TestContext context) {
        vertx = Vertx.vertx();

        vertx.eventBus().addOutboundInterceptor(interceptor("out"));

        String botNickName = "Andy";
        NameGenerator nameGenerator = () -> botNickName;

        Clock clock = Clock.systemDefaultZone();
        BotStorage botStorage = new InMemoryBotStorage();
        StateStorage stateStorage = new InMemoryStateStorage();
        mockBot = mock(IrcBot.class);
        BotFactory botFactory = () -> mockBot;
        EventDispatcher eventDispatcher = new EventbusEventDispatcher(vertx.eventBus(), clock);
        ExitBot exitBot = new ExitBotImpl(botStorage, stateStorage, eventDispatcher);
        InitializeBot initializeBot = new InitializeBotImpl(
                botStorage,
                stateStorage,
                eventDispatcher,
                nameGenerator,
                botFactory
        );
        ToggleDccTransferStarted toggleDccTransferStarted = new ToggleDccTransferStartedImpl(stateStorage);
        Verticle verticle = new NewBotVerticle(initializeBot, exitBot, toggleDccTransferStarted);
        vertx.deployVerticle(verticle, context.asyncAssertSuccess());
    }

    @Test
    @Timeout(value = 3_000)
    void startTransfer_bot_connects_to_irc(TestContext context) {
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

    @Test()
    @Timeout(value = 3_000)
    void stopTransfer_fails_when_bot_missing(TestContext context) {
        JsonObject stopMessage = new JsonObject()
                .put("method", "DELETE")
                .put("params", new JsonObject().put("botname", "missing"));

        vertx.eventBus()
                .request(stopAddress, stopMessage, context.asyncAssertFailure(
                        reply -> context.assertEquals("bot 'missing' not found", reply.getMessage())
                ));
    }

    @Test()
    @Timeout(value = 3_000)
    void stopTransfer_via_request(TestContext context) {
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

    @Test()
    @Timeout(value = 3_000)
    void stopTransfer_because_dcc_transfer_finished(TestContext context) {
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

        DccFinishedEvent dccFinishedEvent = new DccFinishedEvent("Andy");
        JsonObject dccFinishJsonObject = JsonObject.mapFrom(dccFinishedEvent);
        vertx.eventBus()
                .publish(Address.DCC_FINISHED.address(), dccFinishJsonObject);

    }

    @Test()
    @Timeout(value = 3_000)
    void stopTransfer_because_dcc_transfer_failed(TestContext context) {
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

        DccFailedEvent dccFailedEvent = new DccFailedEvent("Andy", "no space on filesystem");
        JsonObject dccFinishJsonObject = JsonObject.mapFrom(dccFailedEvent);
        vertx.eventBus()
                .publish(Address.DCC_FAILED.address(), dccFinishJsonObject);

    }

    @Test()
    @Timeout(value = 3_000)
    void stopTransfer_via_request_cancels_transfer_when_dcc_transfer_started(TestContext context) {
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
                    context.assertEquals("Bot Andy exiting because requested shutdown", body.getString("message"));
                    async.complete();
                });

        // trigger
        DccStartedEvent dccStartedEvent = new DccStartedEvent("Andy");
        JsonObject dccStartedJsonObject = JsonObject.mapFrom(dccStartedEvent);
        vertx.eventBus()
                .publish(Address.DCC_STARTED.address(), dccStartedJsonObject);

        JsonObject stopMessage = new JsonObject()
                .put("method", "DELETE")
                .put("params", new JsonObject().put("botname", "Andy"));

        System.out.printf("serialized https request: %s\n", stopMessage.encode());
        vertx.eventBus()
                .<JsonObject>request(stopAddress, stopMessage, context.asyncAssertSuccess(reply -> {
                    JsonObject body = reply.body();
                    context.assertEquals("Andy", body.getString("bot"));
                    System.out.printf("stop request reply: %s\n", body.encode());
                    context.verify(unused -> verify(mockBot).cancelDcc("remoteBotName"));
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
