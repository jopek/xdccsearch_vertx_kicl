package com.lxbluem.state.domain.model;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.ArrayList;

import static com.lxbluem.state.domain.model.DccState.INIT;
import static org.junit.jupiter.api.Assertions.assertEquals;

class StateTest {

    private final Instant fixedInstant = Instant.parse("2020-08-10T10:11:22Z");
    private final Clock clock = Clock.fixed(fixedInstant, ZoneId.systemDefault());

    State s1;
    State s2;

    @BeforeEach
    void setUp() throws Exception {
        s1 = State.builder()
                .movingAverage(new MovingAverage(1))
                .dccState(INIT)
                .botState(BotState.RUN)
                .oldBotNames(new ArrayList<>())
                .messages(new ArrayList<>())
                .startedTimestamp(Instant.now(clock).toEpochMilli())
                .build();
        System.out.println(s1);

        s2 = State.builder()
                .movingAverage(new MovingAverage(1))
                .dccState(INIT)
                .botState(BotState.RUN)
                .oldBotNames(new ArrayList<>())
                .messages(new ArrayList<>())
                .startedTimestamp(Instant.now(clock).toEpochMilli())
                .build();
        System.out.println(s2);
    }

    @Test
    void testEquals() {
        assertEquals(s1, s2);
    }

    @Test
    void testHashCode() {
        assertEquals(s1.hashCode(), s2.hashCode());
    }
}
