package com.lxbluem.irc;

import com.lxbluem.common.adapter.EventBusBotMessaging;
import com.lxbluem.common.domain.ports.BotMessaging;
import com.lxbluem.irc.adapters.InMemoryBotStateStorage;
import com.lxbluem.irc.adapters.InMemoryBotStorage;
import com.lxbluem.irc.domain.BotService;
import com.lxbluem.irc.domain.ports.*;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.unit.Async;
import io.vertx.ext.unit.TestContext;
import io.vertx.ext.unit.junit.VertxUnitRunner;
import lombok.Data;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.time.Clock;
import java.util.Arrays;
import java.util.Iterator;

import static org.mockito.Mockito.mock;

@RunWith(VertxUnitRunner.class)
public class NewBotVerticleDeploymentTest {

    private Vertx vertx;
    private BotPort mockBot;
    private NewBotVerticle verticle;

    @Before
    public void setUp() {
        vertx = Vertx.vertx();

        String botNickName = "Andy";
        NameGenerator nameGenerator = () -> botNickName;

        Clock clock = Clock.systemDefaultZone();
        BotMessaging botMessaging = new EventBusBotMessaging(vertx.eventBus(), clock);
        BotStorage botStorage = new InMemoryBotStorage();
        DccBotStateStorage stateStorage = new InMemoryBotStateStorage();
        mockBot = mock(BotPort.class);
        BotFactory botFactory = ignored -> mockBot;
        BotService botService = new BotService(botStorage, stateStorage, botMessaging, botFactory, clock, nameGenerator);
        verticle = new NewBotVerticle(botService);
    }

    @Test(timeout = 30_000)
    public void register_route(TestContext context) {
        @Data
        class ExpectedRouteRegistry {
            final String path;
            final String target;
            final String method;
        }
        Iterator<ExpectedRouteRegistry> expectedIterator = Arrays.asList(
                new ExpectedRouteRegistry("/xfers", "NewBotVerticle:POST:/xfers", "POST"),
                new ExpectedRouteRegistry("/xfers", "NewBotVerticle:DELETE:/xfers/:botname", "DELETE")
        ).iterator();

        Async botConnect = context.async();
        vertx.eventBus()
                .<JsonObject>consumer("route", m -> {
                    botConnect.complete();
                    JsonObject body = m.body();
                    ExpectedRouteRegistry expected = expectedIterator.next();
                    context.assertEquals(expected.getPath(), body.getString("path"));
                    context.assertEquals(expected.getTarget(), body.getString("target"));
                    context.assertEquals(expected.getMethod(), body.getString("method"));
                });

        vertx.deployVerticle(verticle, context.asyncAssertSuccess());
        botConnect.await();
    }
}