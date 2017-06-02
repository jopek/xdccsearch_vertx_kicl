package com.lxbluem.stats;

import io.reactivex.Flowable;
import io.vertx.core.json.JsonObject;
import io.vertx.rxjava.core.AbstractVerticle;
import io.vertx.rxjava.core.eventbus.Message;
import rx.Observable;
import rx.Scheduler;
import rx.Subscription;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

public class StatsVerticle extends AbstractVerticle {
    private Map<JsonObject, MovingAverage> averageMap = new HashMap<>();

    private static final int AVG_SIZE = 2;

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
                            JsonObject pack = body.getJsonObject("pack");

                            MovingAverage movingAverage = averageMap.getOrDefault(pack, new MovingAverage(AVG_SIZE));
                            movingAverage.addValue(new Progress(bytes, millis));
                            averageMap.putIfAbsent(pack, movingAverage);

                            MovingAverage.CircularList<Progress> bins = movingAverage.getBins();
                            System.out.println(bins);

                            vertx.eventBus().publish("stats", new JsonObject()
                                    .put("speed.averge", movingAverage.average())
                                    .put("bins", movingAverage.getBins().size())
                            );
                        }
                );
    }
}
