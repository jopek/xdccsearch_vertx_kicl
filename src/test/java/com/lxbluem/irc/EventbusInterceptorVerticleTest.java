package com.lxbluem.irc;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.eventbus.DeliveryContext;
import io.vertx.core.eventbus.EventBus;
import io.vertx.ext.unit.Async;
import io.vertx.ext.unit.TestContext;
import io.vertx.ext.unit.junit.RunTestOnContext;
import io.vertx.ext.unit.junit.VertxUnitRunner;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(VertxUnitRunner.class)
public class EventbusInterceptorVerticleTest {
    @Rule
    public RunTestOnContext rule = new RunTestOnContext();

    @Test(timeout = 5000)
    public void name(TestContext context) {
        Vertx vertx = rule.vertx();
        vertx.eventBus().addOutboundInterceptor(interceptor("out"));
        vertx.eventBus().addInboundInterceptor(interceptor("in"));

        TestVerticle testVerticle = new TestVerticle();
        Async async = context.async();
        vertx.deployVerticle(testVerticle, context.asyncAssertSuccess(v -> async.complete()));
        vertx.eventBus()
                .request("topic", "TEST", context.asyncAssertSuccess(m -> System.out.println(m.body())));

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
        public void start(Future<Void> startFuture) throws Exception {
            EventBus eventBus = vertx.eventBus();
            eventBus.consumer("topic")
                    .handler(message -> {
                        System.out.println(message.body());
                        message.reply("REPLY LA");
                    })
                    .completionHandler(startFuture);
//            startFuture.complete();
        }
    }
}