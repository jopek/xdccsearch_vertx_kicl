package com.lxbluem.stats;

import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.rxjava.core.AbstractVerticle;
import io.vertx.rxjava.core.TimeoutStream;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.lxbluem.stats.BotState.*;

public class StatsVerticle extends AbstractVerticle {
    private Map<JsonObject, StatsVerticleState> stateMap = new HashMap<>();

    private static final int AVG_SIZE_SEC = 5;


    @Override
    public void start() throws Exception {
        subBotDccProgress();
        subBotDccStart();
        subBotDccFinish();
        subBotNotice();

        setupStatsInterval();
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

                            getState(pack);

                            MovingAverage movingAverage = stateMap.get(pack).getMovingAverage();
                            movingAverage.addValue(new Progress(bytes, millis));

                            List<BotState> list = stateMap.get(pack).getBotstates();
                            BotState state = PROGRESS;
                            state.setTimestamp(millis);
                            list.add(state);
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

                            getState(pack);

                            StatsVerticleState state = stateMap.get(pack);
                            state.setStarted(timestamp);

                            List<BotState> botStates = state.getBotstates();
                            BotState botState = START;
                            botState.setTimestamp(timestamp);
                            botStates.add(botState);
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

                            getState(pack);

                            List<BotState> list = stateMap.get(pack).getBotstates();
                            BotState state = FINISH;
                            state.setTimestamp(timestamp);
                            list.add(state);
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

                            getState(pack);

                            List<String> messageList = stateMap.get(pack).getNotices();
                            messageList.add(message);
                        }
                );
    }

    private void getState(JsonObject key) {
        stateMap.putIfAbsent(key, StatsVerticleState.builder()
                .movingAverage(new MovingAverage(AVG_SIZE_SEC))
                .botstates(new ArrayList<>())
                .notices(new ArrayList<>())
                .build());
    }

    private TimeoutStream setupStatsInterval() {
        return vertx.periodicStream(5000)
                .handler(h -> {
                    JsonArray bots = new JsonArray();

                    stateMap.forEach((pack, state) -> {
                        List<BotState> botstates = state.getBotstates();
                        BotState latestBotState = botstates.get(botstates.size() - 1);

                        JsonObject bot = new JsonObject()
                                .put("started", state.getStarted())
                                .put("duration", latestBotState.getTimestamp() - state.getStarted())
                                .put("speed", state.getMovingAverage().average())
                                .put("state", latestBotState)
                                .put("notices", state.getNotices());
                        bots.add(bot);
                    });

                    JsonObject message = new JsonObject()
                            .put("bots", bots);

                    vertx.eventBus().publish("stats", message);
                });
    }

}
