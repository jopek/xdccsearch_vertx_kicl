package com.lxbluem.irc;

import com.lxbluem.common.adapter.EventbusEventDispatcher;
import com.lxbluem.irc.adapters.InMemoryBotStorage;
import com.lxbluem.irc.adapters.InMemoryStateStorage;
import com.lxbluem.irc.domain.interactors.ExitBotImpl;
import com.lxbluem.irc.domain.interactors.InitializeBotImpl;
import com.lxbluem.irc.domain.interactors.ToggleDccTransferStartedImpl;
import com.lxbluem.irc.domain.ports.incoming.ExitBot;
import com.lxbluem.irc.domain.ports.incoming.InitializeBot;
import com.lxbluem.irc.domain.ports.incoming.ToggleDccTransferStarted;
import com.lxbluem.irc.domain.ports.outgoing.*;
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
    private IrcBot mockBot;
    private NewBotVerticle verticle;

    @Before
    public void setUp() {
        vertx = Vertx.vertx();

        String botNickName = "Andy";
        NameGenerator nameGenerator = () -> botNickName;

        Clock clock = Clock.systemDefaultZone();
        BotStorage botStorage = new InMemoryBotStorage();
        StateStorage stateStorage = new InMemoryStateStorage();
        mockBot = mock(IrcBot.class);
        BotFactory botFactory = () -> mockBot;
        EventbusEventDispatcher eventDispatcher = new EventbusEventDispatcher(vertx.eventBus(), clock);
        ExitBot exitBot = new ExitBotImpl(botStorage, stateStorage, eventDispatcher);
        InitializeBot initializeBot = new InitializeBotImpl(
                botStorage,
                stateStorage,
                eventDispatcher,
                nameGenerator,
                botFactory
        );
        ToggleDccTransferStarted toggleDccTransferStarted = new ToggleDccTransferStartedImpl(stateStorage);
        verticle = new NewBotVerticle(initializeBot, exitBot, toggleDccTransferStarted);
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
                new ExpectedRouteRegistry("/xfers/:botname", "NewBotVerticle:DELETE:/xfers/:botname", "DELETE")
        ).iterator();

        Async botConnect = context.async(2);
        vertx.eventBus()
                .<JsonObject>consumer("route", m -> {
                    botConnect.countDown();
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