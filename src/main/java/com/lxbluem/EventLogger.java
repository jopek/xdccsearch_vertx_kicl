package com.lxbluem;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.eventbus.Message;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class EventLogger extends AbstractVerticle {

    private static Logger LOG = LoggerFactory.getLogger(EventLogger.class);

    @Override
    public void start() throws Exception {
        EventBus eventBus = vertx.eventBus();

        eventBus.addInboundInterceptor(handler -> {
                    Message message = handler.message();
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
