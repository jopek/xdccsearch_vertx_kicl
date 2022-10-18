package com.lxbluem.irc.domain.interactors;

import com.lxbluem.irc.domain.ports.outgoing.ScheduledTaskExecution;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AScheduledTaskExecutionImplTest {

    private AtomicBoolean taskRan;
    private ScheduledTaskExecution taskExecution;

    @BeforeEach
    void setUp() {
        taskRan = new AtomicBoolean(false);
        taskExecution = new ScheduledTaskExecutionImpl();
    }

    @Test
    void executes_task_when_scheduled() throws InterruptedException {
        taskExecution.scheduleTask("A", () -> taskRan.set(true), 3, TimeUnit.MILLISECONDS);
        assertFalse(taskRan.get());
        Thread.sleep(10L);
        assertTrue(taskRan.get());
        taskRan.set(false);
    }

    @Test
    void does_not_execute_when_stopped_before_schedule() throws InterruptedException {
        taskExecution.scheduleTask("B", () -> taskRan.set(true), 3, TimeUnit.MILLISECONDS);
        taskExecution.stop("B");
        assertFalse(taskRan.get());
        Thread.sleep(10L);
        assertFalse(taskRan.get());
        taskRan.set(false);
    }
}
