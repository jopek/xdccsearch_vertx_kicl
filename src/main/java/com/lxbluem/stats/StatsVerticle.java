package com.lxbluem.stats;

import io.vertx.core.json.JsonObject;
import io.vertx.rxjava.core.AbstractVerticle;

public class StatsVerticle extends AbstractVerticle {
    @Override
    public void start() throws Exception {
        vertx.eventBus()
                .consumer("bot.dcc.progress")
                .toObservable()
                .subscribe(
                        msg -> {
                            JsonObject body = (JsonObject) msg.body();
                            long bytes = body.getLong("bytes", 0L);
                            long millis = body.getLong("timestamp", 1L);
                            System.out.println(bytes + " BYTES / " + millis + " ms");
                        }
                );
    }
}
