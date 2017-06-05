package com.lxbluem.stats;

import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.rxjava.core.AbstractVerticle;
import io.vertx.rxjava.core.TimeoutStream;
import lombok.*;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

import static com.lxbluem.stats.BotState.*;

public class StatsVerticle extends AbstractVerticle {
    private Map<JsonObject, State> averageSpeedMap = new HashMap<>();
    private Map<JsonObject, List<BotState>> botStateMap = new HashMap<>();
    private Map<JsonObject, List<String>> botNoticeMap = new HashMap<>();
    private Map<JsonObject, State> stateMap = new HashMap<>();

    private AtomicInteger counter = new AtomicInteger();

    private static final int AVG_SIZE_SEC = 5;

    @Getter
    @Setter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    private class State {
        private MovingAverage movingAverage;
        private List<BotState> botstates;
        private List<String> notices;
        private long started;
        private long ended;
    }


    @Override
    public void start() throws Exception {
        subBotDccProgress();
        subBotDccStart();
        subBotDccFinish();
        subBotNotice();

        TimeoutStream timeoutStream = vertx.periodicStream(5000)
                .handler(h -> {
                    JsonArray bots = new JsonArray();

                    stateMap.forEach((pack, state) -> {
                        JsonObject bot = new JsonObject()
                                .put("started", new Date(state.getStarted()))
                                .put("duration", state.getEnded() - state.getStarted())
                                .put("speed", state.getMovingAverage().average())
                                .put("state", state.getBotstates().get(botStateMap.get(pack).size() - 1))
                                .put("notices", state.getNotices());
                        bots.add(bot);
                    });

                    JsonObject message = new JsonObject()
                            .put("bots", bots)
                            .put("cnt", counter.incrementAndGet());

                    vertx.eventBus().publish("stats", message);
                });


    }

    private void subBotDccProgress() {
        vertx.eventBus()
                .consumer("bot.dcc.progress")
                .toObservable()
                .subscribe(
                        msg -> {
                            JsonObject body = (JsonObject) msg.body();
                            long bytes = body.getLong("bytes", 0L);
                            long millis = body.getLong("timestamp", 1L);
                            JsonObject pack = body.getJsonObject("pack");

//                            stateMap.putIfAbsent(pack, State.builder().movingAverage().build());

                            MovingAverage movingAverage = stateMap.get(pack).getMovingAverage();
                            movingAverage.addValue(new Progress(bytes, millis));
//                            averageMap.putIfAbsent(pack, movingAverage);

                            List<BotState> list = stateMap.get(pack).getBotstates();
                            BotState state = PROGRESS;
                            state.setTimestamp(millis);
                            list.add(state);
                            botStateMap.putIfAbsent(pack, list);
                        }
                );
    }

    private void subBotDccStart() {
        vertx.eventBus()
                .consumer("bot.dcc.start")
                .toObservable()
                .subscribe(
                        msg -> {
                            JsonObject body = (JsonObject) msg.body();
                            JsonObject pack = body.getJsonObject("pack");
                            long timestamp = body.getLong("timestamp");

                            List<BotState> list = botStateMap.getOrDefault(pack, new ArrayList<>());
                            BotState state = START;
                            state.setTimestamp(timestamp);
                            list.add(state);
                            botStateMap.putIfAbsent(pack, list);
                        }
                );
    }

    private void subBotDccFinish() {
        vertx.eventBus()
                .consumer("bot.dcc.finish")
                .toObservable()
                .subscribe(
                        msg -> {
                            JsonObject body = (JsonObject) msg.body();
                            JsonObject pack = body.getJsonObject("pack");
                            long timestamp = body.getLong("timestamp");

                            List<BotState> list = botStateMap.getOrDefault(pack, new ArrayList<>());
                            BotState state = FINISH;
                            state.setTimestamp(timestamp);
                            list.add(state);
                            botStateMap.putIfAbsent(pack, list);
                        }
                );
    }

    private void subBotNotice() {
        vertx.eventBus()
                .consumer("bot.notice")
                .toObservable()
                .subscribe(
                        msg -> {
                            JsonObject body = (JsonObject) msg.body();
                            JsonObject pack = body.getJsonObject("pack");
                            String message = body.getString("message");

                            List<String> messageList = botNoticeMap.getOrDefault(pack, new ArrayList<>());
                            messageList.add(message);
                            botNoticeMap.putIfAbsent(pack, messageList);
                        }
                );
    }

}
