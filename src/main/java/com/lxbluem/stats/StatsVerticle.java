package com.lxbluem.stats;

import com.lxbluem.AbstractRouteVerticle;
import com.lxbluem.model.SerializedRequest;
import io.vertx.core.Future;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.rxjava.core.eventbus.Message;
import rx.functions.Action1;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static com.lxbluem.stats.DccState.*;
import static io.vertx.core.http.HttpMethod.GET;

public class StatsVerticle extends AbstractRouteVerticle {
    private Map<JsonObject, State> stateMap = new HashMap<>();

    private static final int AVG_SIZE_SEC = 5;


    @Override
    public void start() throws Exception {
        registerRouteWithHandler(GET, "/stats", this::getStats);

        handle("bot.init", this::init);
        handle("bot.dcc.start", this::dccStart);
        handle("bot.dcc.progress", this::dccProgress);
        handle("bot.dcc.finish", this::dccFinish);
        handle("bot.notice", this::notice);

        setupStatsInterval();
    }

    private void getStats(SerializedRequest serializedRequest, Future<JsonObject> jsonObjectFuture) {
        jsonObjectFuture.complete(getEntries());
    }

    private void handle(String address, Action1<Message<JsonObject>> method) {
        vertx.eventBus()
                .<JsonObject>consumer(address)
                .toObservable()
                .subscribe(method);
    }

    private void init(Message<JsonObject> eventMessage) {
        JsonObject body = eventMessage.body();
        JsonObject pack = body.getJsonObject("pack");
        long timestamp = body.getLong("timestamp");

        State state = updateVerticleState(pack, timestamp);
        addBotState(state, INIT, timestamp);
    }

    private void dccStart(Message<JsonObject> eventMessage) {
        JsonObject body = eventMessage.body();
        JsonObject pack = body.getJsonObject("pack");
        long timestamp = body.getLong("timestamp");

        State state = updateVerticleState(pack, timestamp);
        addBotState(state, START, timestamp);
    }

    private void dccProgress(Message<JsonObject> eventMessage) {
        JsonObject body = eventMessage.body();
        long bytes = body.getLong("bytes", 0L);
        long timestamp = body.getLong("timestamp", 1L);
        JsonObject pack = body.getJsonObject("pack");

        State state = updateVerticleState(pack, timestamp);

        MovingAverage movingAverage = state.getMovingAverage();
        movingAverage.addValue(new Progress(bytes, timestamp));

        List<DccState> dccStates = state
                .getDccStates()
                .stream()
                .filter(bs -> !bs.equals(PROGRESS))
                .collect(Collectors.toList());
        DccState dccState = PROGRESS;
        dccState.setTimestamp(timestamp);
        dccStates.add(dccState);
        state.setDccStates(dccStates);
    }

    private void dccFinish(Message<JsonObject> eventMessage) {
        JsonObject body = eventMessage.body();
        JsonObject pack = body.getJsonObject("pack");
        long timestamp = body.getLong("timestamp");

        State state = updateVerticleState(pack, timestamp);
        addBotState(state, FINISH, timestamp);
    }

    private void notice(Message<JsonObject> eventMessage) {
        JsonObject body = eventMessage.body();
        JsonObject pack = body.getJsonObject("pack");
        String message = body.getString("message");
        long timestamp = body.getLong("timestamp");

        State state = updateVerticleState(pack, timestamp);

        List<String> messageList = state.getNotices();
        messageList.add(message);
    }

    private State updateVerticleState(JsonObject pack, long timestamp) {
        stateMap.putIfAbsent(pack, State.builder()
                .movingAverage(new MovingAverage(AVG_SIZE_SEC))
                .dccStates(new ArrayList<>())
                .notices(new ArrayList<>())
                .build());
        State state = stateMap.get(pack);
        state.setStarted(timestamp);
        return state;
    }

    private void addBotState(State state, DccState dccState, Long timestamp) {
        dccState.setTimestamp(timestamp);

        List<DccState> dccStates = state.getDccStates();
        dccStates.add(dccState);
    }

    private void setupStatsInterval() {
        vertx.periodicStream(5000)
                .handler(h -> {
                    JsonObject message = getEntries();
                    vertx.eventBus().publish("stats", message);
                });
    }

    private JsonObject getEntries() {
        JsonArray bots = new JsonArray();

        stateMap.forEach((pack, state) -> {
            List<DccState> dccStates = state.getDccStates();
            int botStatesSize = dccStates.size();
            if (botStatesSize == 0)
                return;

            DccState latestDccState = dccStates.get(botStatesSize - 1);

            JsonObject bot = new JsonObject()
                    .put("pack", pack)
                    .put("started", state.getStarted())
                    .put("duration", latestDccState.getTimestamp() - state.getStarted())
                    .put("speed", state.getMovingAverage().average())
                    .put("state", latestDccState)
                    .put("notices", state.getNotices());
            bots.add(bot);
        });

        return new JsonObject().put("bots", bots);
    }

}
