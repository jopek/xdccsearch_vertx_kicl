package com.lxbluem.irc;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.Handler;
import io.vertx.core.Promise;
import io.vertx.core.Verticle;
import io.vertx.core.Vertx;
import io.vertx.core.eventbus.DeliveryContext;
import io.vertx.core.eventbus.EventBus;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.api.extension.ExtendWith;

import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertTrue;

@ExtendWith(VertxExtension.class)
class EventbusInterceptorVerticleTest {
    @Test
    @Timeout(5)
    void interceptMessages(VertxTestContext context) throws InterruptedException {
        Vertx vertx = Vertx.vertx();
        vertx.eventBus().addOutboundInterceptor(interceptor("out"));
        vertx.eventBus().addInboundInterceptor(interceptor("in"));
        Verticle testVerticle = new TestVerticle();
        vertx.deployVerticle(testVerticle, context.succeedingThenComplete());

        vertx.eventBus()
                .request("topic", "TEST", context.succeedingThenComplete());

        context.awaitCompletion(100, TimeUnit.MILLISECONDS);

        assertTrue(context.completed());
    }

    private Handler<DeliveryContext<Object>> interceptor(String direction) {
        return dc -> {
            Object body = dc.body();
            String address = dc.message().address();
            String sendPublish = dc.message().isSend() ? "send" : "publish";
            System.out.printf("%s INTERCEPTOR %s %s %s\n", direction, sendPublish, address, body);
            dc.next();
        };
    }

    private static class TestVerticle extends AbstractVerticle {
        @Override
        public void start(Promise<Void> startFuture) {
            EventBus eventBus = vertx.eventBus();
            eventBus.consumer("topic")
                    .handler(message -> {
                        System.out.println(message.body());
                        message.reply("REPLY LA");
                    })
                    .completionHandler(startFuture);
        }
    }
}
