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
import com.lxbluem.irc.domain.ports.outgoing.BotFactory;
import com.lxbluem.irc.domain.ports.outgoing.BotStorage;
import com.lxbluem.irc.domain.ports.outgoing.IrcBot;
import com.lxbluem.irc.domain.ports.outgoing.NameGenerator;
import com.lxbluem.irc.domain.ports.outgoing.StateStorage;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.junit5.Checkpoint;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;
import lombok.Data;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.api.extension.ExtendWith;

import java.time.Clock;
import java.util.Arrays;
import java.util.Iterator;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

@ExtendWith(VertxExtension.class)
class NewBotVerticleDeploymentTest {

    private Vertx vertx;
    private IrcBot mockBot;
    private NewBotVerticle verticle;

    @BeforeEach
    void setUp() {
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

    @Test
    @Timeout(value = 3)
    void register_route(VertxTestContext context) {
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

        Checkpoint botConnect = context.checkpoint(2);
        vertx.eventBus()
                .<JsonObject>consumer("route", m -> {
                    botConnect.flag();
                    JsonObject body = m.body();
                    ExpectedRouteRegistry expected = expectedIterator.next();
                    context.verify(() -> assertThat(body)
                            .isEqualTo(JsonObject.mapFrom(expected))
                    );
                });

        vertx.deployVerticle(verticle, context.succeedingThenComplete());
    }
}
