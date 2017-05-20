package com.lxbluem;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.eventbus.Message;

public class EventLogger extends AbstractVerticle {

  @Override
  public void start() throws Exception {
    EventBus eventBus = vertx.eventBus();

    eventBus.addInterceptor(handler -> {
          Message message = handler.message();
          System.out.printf("E A:[%s] RA:[%s] H:[%s] B:[%s]\n",
              message.address(),
              message.replyAddress(),
              message.headers(),
              message.body());
          handler.next();
        }
    );
  }
}
