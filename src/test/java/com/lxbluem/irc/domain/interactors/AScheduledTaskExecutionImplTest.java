package com.lxbluem.irc.domain.interactors;

import org.junit.Before;
import org.junit.Test;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class AScheduledTaskExecutionImplTest {

    private AtomicBoolean taskRan;
    private ScheduledTaskExecutionImpl taskExecution;

    @Before
    public void setUp() {
        taskRan = new AtomicBoolean(false);
        taskExecution = new ScheduledTaskExecutionImpl();
    }

    @Test
    public void executes_task_when_scheduled() throws InterruptedException {
        taskExecution.scheduleTask("A", ()->taskRan.set(true), 3, TimeUnit.MILLISECONDS);
        assertFalse(taskRan.get());
        Thread.sleep(10L);
        assertTrue(taskRan.get());
        taskRan.set(false);
    }

    @Test
    public void does_not_execute_when_stopped_before_schedule() throws InterruptedException {
        taskExecution.scheduleTask("B", ()->taskRan.set(true), 3, TimeUnit.MILLISECONDS);
        taskExecution.stop("B");
        assertFalse(taskRan.get());
        Thread.sleep(10L);
        assertFalse(taskRan.get());
        taskRan.set(false);
    }
}