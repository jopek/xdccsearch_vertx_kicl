package com.lxbluem.eventlogger;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.eventbus.Message;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class EventLoggerVerticle extends AbstractVerticle {

    private static final Logger LOG = LoggerFactory.getLogger(EventLoggerVerticle.class);

    @Override
    public void start() throws Exception {
        EventBus eventBus = vertx.eventBus();

        eventBus.addOutboundInterceptor(handler -> {
                    Message<Object> message = handler.message();
                    LOG.trace("A:[{}] RA:[{}] H:[{}] B:[{}]",
                            message.address(),
                            message.replyAddress(),
                            message.headers(),
                            message.body());
                    handler.next();
                }
        );
    }
}
