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
import io.vertx.junit5.Checkpoint;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.api.extension.ExtendWith;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

@ExtendWith(VertxExtension.class)
class NewBotVerticleTest {


    public static final JsonObject PACK = new JsonObject()
            .put("cname", "#download")
            .put("n", 1)
            .put("name", "lala")
            .put("gets", 0)
            .put("nname", "local")
            .put("naddr", "192.168.99.100")
            .put("nport", 6668)
            .put("uname", "remoteBotName")
            .put("sz", 0)
            .put("szf", null)
            .put("age", 0)
            .put("agef", null)
            .put("last", 0)
            .put("lastf", null);
    public static final Instant INSTANT = Instant.ofEpochMilli(1666650521368L);
    private static final String START_ADDRESS = "NewBotVerticle:POST:/xfers";
    private static final String STOP_ADDRESS = "NewBotVerticle:DELETE:/xfers/:botname";
    private static final JsonObject START_MESSAGE = new JsonObject()
            .put("method", "POST")
            .put("body", PACK.encode());
    private IrcBot mockBot;

    @BeforeEach
    void setUp(VertxTestContext context, Vertx vertx) {
        vertx.eventBus().addOutboundInterceptor(interceptor("out"));

        String botNickName = "Andy";
        NameGenerator nameGenerator = () -> botNickName;

        Clock clock = Clock.fixed(INSTANT, ZoneId.systemDefault());
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

        Checkpoint checkpoint = context.checkpoint();
        vertx.deployVerticle(verticle, ignored -> checkpoint.flag());
    }

    @Test
    @Timeout(value = 3_000)
    void startTransfer_bot_connects_to_irc(VertxTestContext context, Vertx vertx) throws Throwable {
        vertx.eventBus()
                .<JsonObject>request(START_ADDRESS, START_MESSAGE, context.succeeding(m ->
                        assertThat(m.body().getString("bot")).isEqualTo("Andy")));

        BotConnectionDetails expected = new BotConnectionDetails(
                "192.168.99.100",
                6668,
                "Andy",
                "name_Andy",
                "user_Andy",
                "realname_Andy");
        Checkpoint checkpoint = context.checkpoint();
        vertx.eventBus().consumer("bot.init", message -> {
            checkpoint.flag();
        });
        context.awaitCompletion(4, TimeUnit.SECONDS);
        context.verify(() -> {
            verify(mockBot).connect(expected);
            verify(mockBot).joinChannel("#download");
        });
    }

    @Test
    @Timeout(value = 3_000)
    void startTransfer_sends_event(VertxTestContext context, Vertx vertx) throws InterruptedException {
        vertx.eventBus()
                .<JsonObject>request(START_ADDRESS, START_MESSAGE, context.succeeding(m ->
                        assertThat(m.body().getString("bot")).isEqualTo("Andy")));

        JsonObject expectedMessage = new JsonObject()
                .put("timestamp", INSTANT.toEpochMilli())
                .put("bot", "Andy")
                .put("pack", PACK);

        Checkpoint checkpoint = context.checkpoint();
        vertx.eventBus().consumer("bot.init", message -> {
            checkpoint.flag();
            context.verify(
                    () -> assertThat(message.body()).isEqualTo(expectedMessage)
            );
        });
    }

    @Test()
    @Timeout(value = 3_000)
    void stopTransfer_fails_when_bot_missing(VertxTestContext context, Vertx vertx) {
        JsonObject failingMessage = new JsonObject()
                .put("method", "DELETE")
                .put("params", new JsonObject().put("botname", "missing"));

        vertx.eventBus()
                .request(STOP_ADDRESS, failingMessage, context.failing(
                        reply -> {
                            assertThat(reply).hasMessage("bot 'missing' not found");
                            context.completeNow();
                        }
                ));
    }

    @Test()
    @Timeout(value = 3_000)
    void stopTransfer_via_request(VertxTestContext context, Vertx vertx) {
        vertx.eventBus()
                .<JsonObject>request(START_ADDRESS, START_MESSAGE, context.succeeding(m ->
                        assertThat(m.body().getString("bot")).isEqualTo("Andy"))
                );

        Checkpoint messageCp = context.checkpoint();
        vertx.eventBus()
                .<JsonObject>consumer(Address.BOT_EXITED.address(), result -> {
                    JsonObject body = result.body();
                    context.verify(() -> {
                        assertThat(body.getMap().keySet()).containsAll(List.of("bot", "timestamp", "message"));
                        assertThat(body.getString("bot")).isEqualTo("Andy");
                    });
                    messageCp.flag();
                });

        // trigger
        JsonObject stopMessage = new JsonObject()
                .put("method", "DELETE")
                .put("params", new JsonObject().put("botname", "Andy"));

        System.out.printf("serialized https request: %s\n", stopMessage.encode());
        Checkpoint replyCp = context.checkpoint();
        vertx.eventBus()
                .<JsonObject>request(STOP_ADDRESS, stopMessage, context.succeeding(reply -> {
                    JsonObject body = reply.body();
                    assertThat(body.getString("bot")).isEqualTo("Andy");
                    replyCp.flag();
                }));
    }

    @Test()
    @Timeout(value = 3_000)
    void stopTransfer_because_dcc_transfer_finished(VertxTestContext context, Vertx vertx) {
        vertx.eventBus()
                .<JsonObject>request(START_ADDRESS, START_MESSAGE, context.succeeding(m ->
                        assertThat(m.body().getString("bot")).isEqualTo("Andy")));

        Checkpoint messageCp = context.checkpoint();
        vertx.eventBus()
                .consumer(Address.BOT_EXITED.address(), result -> {
                    JsonObject body = (JsonObject) result.body();
                    System.out.println(body.encode());
                    assertThat(body.getString("bot")).isEqualTo("Andy");
                    assertThat(body.getString("message")).isEqualTo("Bot Andy exiting because DCC transfer finished");
                    messageCp.flag();
                });

        DccFinishedEvent dccFinishedEvent = new DccFinishedEvent("Andy");
        JsonObject dccFinishJsonObject = JsonObject.mapFrom(dccFinishedEvent);
        vertx.eventBus()
                .publish(Address.DCC_FINISHED.address(), dccFinishJsonObject);

    }

    @Test()
    @Timeout(value = 3_000)
    void stopTransfer_because_dcc_transfer_failed(VertxTestContext context, Vertx vertx) {
        vertx.eventBus()
                .<JsonObject>request(START_ADDRESS, START_MESSAGE, context.succeeding(m ->
                        assertThat(m.body().getString("bot")).isEqualTo("Andy")));

        Checkpoint exitAsync = context.checkpoint();
        vertx.eventBus()
                .consumer(Address.BOT_EXITED.address(), result -> {
                    JsonObject body = (JsonObject) result.body();
                    System.out.println(body.encode());
                    assertThat(body.getString("bot")).isEqualTo("Andy");
                    assertThat(body.getString("message")).isEqualTo("Bot Andy exiting because no space on filesystem");
                    exitAsync.flag();
                });

        DccFailedEvent dccFailedEvent = new DccFailedEvent("Andy", "no space on filesystem");
        JsonObject dccFinishJsonObject = JsonObject.mapFrom(dccFailedEvent);
        vertx.eventBus()
                .publish(Address.DCC_FAILED.address(), dccFinishJsonObject);

    }

    @Test()
    @Timeout(value = 3_000)
    void stopTransfer_via_request_cancels_transfer_when_dcc_transfer_started(VertxTestContext context, Vertx vertx) {
        vertx.eventBus()
                .<JsonObject>request(START_ADDRESS, START_MESSAGE, context.succeeding(m ->
                        assertThat(m.body().getString("bot")).isEqualTo("Andy")));

        Checkpoint messageCp = context.checkpoint();
        vertx.eventBus()
                .consumer(Address.BOT_EXITED.address(), result -> {
                    JsonObject body = (JsonObject) result.body();
                    System.out.println(body.encode());
                    assertThat(body.getString("bot")).isEqualTo("Andy");
                    assertThat(body.getString("message")).isEqualTo("Bot Andy exiting because requested shutdown");
                    messageCp.flag();
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
                .<JsonObject>request(STOP_ADDRESS, stopMessage, context.succeeding(reply -> {
                    JsonObject body = reply.body();
                    assertThat(body.getString("bot")).isEqualTo("Andy");
                    System.out.printf("stop request reply: %s\n", body.encode());
                    context.verify(() -> verify(mockBot).cancelDcc("remoteBotName"));
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
