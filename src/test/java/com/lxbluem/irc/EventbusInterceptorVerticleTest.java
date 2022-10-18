package com.lxbluem.irc;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.Verticle;
import io.vertx.core.Vertx;
import io.vertx.core.eventbus.DeliveryContext;
import io.vertx.core.eventbus.EventBus;
import io.vertx.junit5.VertxExtension;
import io.vertx.reactivex.ext.unit.Async;
import io.vertx.reactivex.ext.unit.TestContext;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.api.extension.ExtendWith;

import static org.junit.jupiter.api.Assertions.assertTrue;

@ExtendWith(VertxExtension.class)
class EventbusInterceptorVerticleTest {
    @Test
    @Timeout(5)
    void interceptMessages(TestContext context) {
        Vertx vertx = Vertx.vertx();
        vertx.eventBus().addOutboundInterceptor(interceptor("out"));
        vertx.eventBus().addInboundInterceptor(interceptor("in"));

        Verticle testVerticle = new TestVerticle();
        Async async = context.async();
        vertx.deployVerticle(testVerticle, context.asyncAssertSuccess(v -> async.complete()));
        async.await();
        vertx.eventBus()
                .request("topic", "TEST", context.asyncAssertSuccess(m -> System.out.println(m.body())));

        assertTrue(async.isCompleted());

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
        public void start(Future<Void> startFuture) {
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
