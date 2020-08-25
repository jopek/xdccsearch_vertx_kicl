package com.lxbluem.state;

import com.lxbluem.Address;
import com.lxbluem.domain.Pack;
import com.lxbluem.state.adapters.InMemoryStateRepository;
import com.lxbluem.state.domain.model.State;
import com.lxbluem.state.domain.ports.StateRepository;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.unit.Async;
import io.vertx.ext.unit.TestContext;
import io.vertx.ext.unit.junit.VertxUnitRunner;
import io.vertx.rxjava.core.Vertx;
import lombok.Data;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import static org.junit.Assert.assertEquals;

@RunWith(VertxUnitRunner.class)
public class StateVerticleTest {
    private Vertx vertx;
    private StateVerticle verticle;
    private Clock clock;

    @Before
    public void setUp() throws Exception {
        vertx = Vertx.vertx();
        clock = Clock.fixed(Instant.parse("2020-01-30T18:00:00.00Z"), ZoneId.systemDefault());
        StateRepository stateRepository = new InMemoryStateRepository();
        verticle = new StateVerticle(new StateService(stateRepository, clock));
    }

    @After
    public void tearDown() throws Exception {
    }


    @Test
    public void register_route(TestContext context) {
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

        Async registerRoutes = context.async(2);
        vertx.eventBus()
                .<JsonObject>consumer("route", m -> {
                    registerRoutes.countDown();
                    JsonObject body = m.body();
                    ExpectedRouteRegistry expected = expectedIterator.next();
                    context.assertEquals(expected.getPath(), body.getString("path"));
                    context.assertEquals(expected.getTarget(), body.getString("target"));
                    context.assertEquals(expected.getMethod(), body.getString("method"));
                });

        vertx.deployVerticle(verticle, context.asyncAssertSuccess());
        registerRoutes.await();
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
    public void state_to_json_serialisation() {
        State build = State.builder().build();
        Map<String, State> map = new HashMap<>();
        map.put("a", build);

        JsonObject jsonObject = new JsonObject();
        map.forEach((k, v) -> jsonObject.put(k, JsonObject.mapFrom(v)));
    }

    @Test(timeout = 0_000)
    public void initialise_state(TestContext context) {

        Async stateSent = context.async();
        vertx.eventBus().<JsonObject>consumer(Address.STATE.getAddressValue(), m -> {
            JsonObject body = m.body();
            context.verify(ignored -> assertEquals(expectedInitialState().encodePrettily(), body.encodePrettily()));
            stateSent.complete();
        });
        vertx.deployVerticle(verticle, context.asyncAssertSuccess());

        JsonObject init_message = new JsonObject()
                .put("bot", "Andy")
                .put("timestamp", 123456L)
                .put("pack", JsonObject.mapFrom(testPack()));
        vertx.eventBus().publish(Address.BOT_INIT.getAddressValue(), init_message);

        stateSent.await();
    }

    private JsonObject expectedInitialState() {
        return new JsonObject()
                .put("Andy", new JsonObject()
                        .put("movingAverage", new JsonObject()
                                .put("q", new JsonArray())
                                .put("secondsToSave", 5))
                        .put("dccState", "INIT")
                        .put("oldBotNames", new JsonArray())
                        .put("messages", new JsonArray())
                        .put("timestamp", 123456)
                        .put("endedTimestamp", 0)
                        .put("startedTimestamp", Instant.now(clock).toEpochMilli())
                        .put("pack", new JsonObject()
                                .put("pid", 0)
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
                        .put("filenameOnDisk", "")
                        .put("bytesTotal", 0)
                        .put("bytes", 0)
                        .put("botState", "RUN")
                        .put("botName", "Andy"));
    }
}