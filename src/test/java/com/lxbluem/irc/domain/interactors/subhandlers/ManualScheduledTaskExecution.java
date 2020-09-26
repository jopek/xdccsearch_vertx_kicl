package com.lxbluem.irc.domain.interactors.subhandlers;

import com.lxbluem.irc.domain.ports.outgoing.ScheduledTaskExecution;

import java.util.concurrent.TimeUnit;

public class ManualScheduledTaskExecution implements ScheduledTaskExecution {

    private Runnable task;

    @Override
    public void scheduleTask(String botNickName, Runnable task, long l, TimeUnit minutes) {
        this.task = task;
    }

    @Override
    public void stop(String s) {
    }

    public void runTask() {
        task.run();
    }
}
