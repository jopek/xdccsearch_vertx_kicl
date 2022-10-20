package com.lxbluem.irc.domain.interactors;


import com.lxbluem.irc.domain.ports.outgoing.ScheduledTaskExecution;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

public class ScheduledTaskExecutionImpl implements ScheduledTaskExecution {
    private final ScheduledExecutorService timerService = Executors.newSingleThreadScheduledExecutor();
    private final Map<String, ScheduledFuture<?>> botScheduledTasks = new HashMap<>();

    @Override
    public void scheduleTask(String botNickName, Runnable task, long delay, TimeUnit timeUnit) {
        ScheduledFuture<?> schedule = timerService.schedule(() -> {
            botScheduledTasks.remove(botNickName);
            task.run();
        }, delay, timeUnit);
        botScheduledTasks.put(botNickName, schedule);
    }

    @Override
    public void stop(String botNickName) {
        ScheduledFuture<?> task = botScheduledTasks.remove(botNickName);

        if (task != null)
            task.cancel(false);
    }
}
