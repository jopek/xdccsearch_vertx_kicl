package com.lxbluem.irc.domain.ports.outgoing;

import java.util.concurrent.TimeUnit;

public interface ScheduledTaskExecution {

  void scheduleTask(String botNickName, Runnable task, long l, TimeUnit minutes);

  void stop(String botNickName);
}
