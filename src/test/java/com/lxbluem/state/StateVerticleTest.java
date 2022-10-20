package com.lxbluem.state;

import com.lxbluem.common.domain.Pack;
import com.lxbluem.common.infrastructure.Address;
import com.lxbluem.state.adapters.InMemoryStateRepository;
import com.lxbluem.state.domain.StateService;
import com.lxbluem.state.domain.model.State;
import com.lxbluem.state.domain.ports.StateRepository;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.junit5.Checkpoint;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;
import io.vertx.reactivex.ext.unit.Async;
import io.vertx.reactivex.ext.unit.TestContext;
import io.vertx.rxjava.core.Vertx;
import lombok.Data;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.api.extension.ExtendWith;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;

@ExtendWith(VertxExtension.class)
class StateVerticleTest {
    private Vertx vertx;
    private StateVerticle verticle;
    private Clock clock;

    @BeforeEach
    void setUp() {
        vertx = Vertx.vertx();
        clock = Clock.fixed(Instant.parse("2020-01-30T18:00:00.00Z"), ZoneId.systemDefault());
        StateRepository stateRepository = new InMemoryStateRepository();
        verticle = new StateVerticle(new StateService(stateRepository, clock), clock);
    }

    @AfterEach
    void tearDown() {
    }


    @Test
//    @Timeout(5)
    void register_route(VertxTestContext context) throws InterruptedException {
        @Data
        class ExpectedRouteRegistry {
            final String path;
            final String target;
            final String method;
        }
        Iterator<ExpectedRouteRegistry> expectedIterator = Arrays.asList(
                new ExpectedRouteRegistry("/state", "StateVerticle:DELETE:/state", "DELETE"),
                new ExpectedRouteRegistry("/state", "StateVerticle:GET:/state", "GET")
        ).iterator();

        context.checkpoint(2);
        vertx.eventBus()
                .<JsonObject>consumer("route", m -> {
                    context.checkpoint();
                    JsonObject body = m.body();
                    ExpectedRouteRegistry expected = expectedIterator.next();
                    assertThat(expected)
                            .extracting(
                                    ExpectedRouteRegistry::getPath,
                                    ExpectedRouteRegistry::getTarget,
                                    ExpectedRouteRegistry::getMethod
                            )
                            .containsExactlyInAnyOrder(
                                    body.getString("path"),
                                    body.getString("target"),
                                    body.getString("method")
                            );
                });

        vertx.deployVerticle(verticle, context.completing());
        context.awaitCompletion(100, TimeUnit.MILLISECONDS);
    }

    private Pack testPack() {
        return Pack.builder()
                .nickName("keex")
                .networkName("nn")
                .serverHostName("192.168.99.100")
                .serverPort(6667)
                .channelName("#download")
                .packNumber(5)
                .packName("")
                .sizeFormatted("")
                .ageFormatted("")
                .lastAdvertisedFormatted("")
                .build();
    }

    @Test
    void state_to_json_serialisation() {
        State build = State.builder().build();
        Map<String, State> map = new HashMap<>();
        map.put("a", build);

        JsonObject jsonObject = new JsonObject();
        map.forEach((k, v) -> jsonObject.put(k, JsonObject.mapFrom(v)));

        assertThat(map).isNotEmpty();
    }

    @Test
    @Timeout(value = 30)
    void initialise_state(VertxTestContext context) {

        vertx.eventBus().addOutboundInterceptor(
                dc -> {
                    Object body = dc.body();
                    String address = dc.message().address();
                    String sendPublish = dc.message().isSend() ? "send" : "publish";
                    System.out.printf("%s INTERCEPTOR %s %s\n", sendPublish, address, body);
                    dc.next();
                });

        vertx.eventBus().<JsonObject>consumer(Address.STATE.address(), m -> {
            JsonObject body = m.body();
            context.verify(() -> assertEquals(expectedInitialState().encodePrettily(), body.encodePrettily()));
        });

        Checkpoint deploymentComplete = context.checkpoint();
        vertx.deployVerticle(verticle, context.succeeding(result -> deploymentComplete.flag()));

        JsonObject init_message = new JsonObject()
                .put("bot", "Andy")
                .put("timestamp", 123456L)
                .put("pack", JsonObject.mapFrom(testPack()));
        vertx.eventBus().publish(Address.BOT_INITIALIZED.address(), init_message);

    }

    private JsonObject expectedInitialState() {
        return new JsonObject()
                .put("Andy", new JsonObject()
                        .put("started", Instant.now(clock).toEpochMilli())
                        .put("duration", 0)
                        .put("timestamp", 123456)
                        .put("speed", 0.0)
                        .put("dccstate", "INIT")
                        .put("botstate", "RUN")
                        .put("messages", new JsonArray())
                        .put("oldBotNames", new JsonArray())
                        .put("bot", "Andy")
                        .put("filenameOnDisk", "")
                        .put("bytesTotal", 0)
                        .put("bytes", 0)
                        .put("pack", new JsonObject()
                                .put("cname", "#download")
                                .put("n", 5)
                                .put("name", "")
                                .put("gets", 0)
                                .put("nname", "nn")
                                .put("naddr", "192.168.99.100")
                                .put("nport", 6667)
                                .put("uname", "keex")
                                .put("sz", 0)
                                .put("szf", "")
                                .put("age", 0)
                                .put("agef", "")
                                .put("last", 0)
                                .put("lastf", "")
                        )
                );
    }
}
