package com.lxbluem.state;

import com.lxbluem.AbstractRouteVerticle;
import com.lxbluem.model.SerializedRequest;
import io.vertx.core.Future;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.rxjava.core.eventbus.Message;
import rx.functions.Action1;

import java.util.*;
import java.util.stream.Collectors;

import static com.lxbluem.Addresses.*;
import static com.lxbluem.state.DccState.*;
import static io.vertx.core.http.HttpMethod.GET;

public class StateVerticle extends AbstractRouteVerticle {
    private Map<JsonObject, State> stateMap = new HashMap<>();

    private static final int AVG_SIZE_SEC = 5;


    @Override
    public void start() throws Exception {
        registerRouteWithHandler(GET, "/state", this::getState);
        registerRouteWithHandler(GET, "/state/:packid", this::getStateByPackId);

        handle(BOT_INIT, this::init);
        handle(BOT_NOTICE, this::notice);
        handle(BOT_DCC_START, this::dccStart);
        handle(BOT_DCC_PROGRESS, this::dccProgress);
        handle(BOT_DCC_FINISH, this::dccFinish);

        handle(BOT_INIT, this::wrapMessage);
        handle(BOT_NOTICE, this::wrapMessage);
        handle(BOT_EXIT, this::wrapMessage);
        handle(BOT_DCC_START, this::wrapMessage);
        handle(BOT_DCC_PROGRESS, this::wrapMessage);
        handle(BOT_DCC_FINISH, this::wrapMessage);

//        setupStatePublishInterval();
    }

    private void wrapMessage(Message<JsonObject> event) {
        long pid = event.body()
                .getJsonObject("pack")
                .getLong("pid");

        Optional<Object> state = getStateByPackId(pid);

        JsonObject toBePublished = new JsonObject().put("topic", event.address());

        state.map(stateJsonObject -> (JsonObject)stateJsonObject)
                .ifPresent(o -> vertx.eventBus().publish(STATE, toBePublished.mergeIn(o)));
    }

    private void getStateByPackId(SerializedRequest serializedRequest, Future<JsonObject> jsonObjectFuture) {
        String pidPathParam = serializedRequest.getParams().get("packid");
        if (pidPathParam == null || pidPathParam.isEmpty()) {
            jsonObjectFuture.fail("pack id format");
            return;
        }
        long pid = Long.parseLong(pidPathParam);

        Optional<Object> entry = getStateByPackId(pid);

        if (entry.isPresent())
            jsonObjectFuture.complete((JsonObject) entry.get());
        else
            jsonObjectFuture.fail("not found");
    }

    private Optional<Object> getStateByPackId(long pid) {
        JsonArray stateEntries = getStateEntries();
        return stateEntries
                .stream()
                .filter(o -> {
                    JsonObject jsonObject = (JsonObject) o;
                    JsonObject pack = jsonObject.getJsonObject("pack");

                    return pack.getLong("pid") == pid;
                })
                .findFirst();
    }

    private void getState(SerializedRequest serializedRequest, Future<JsonObject> jsonObjectFuture) {
        jsonObjectFuture.complete(new JsonObject().put("state", getStateEntries()));
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

    private void setupStatePublishInterval() {
        vertx.periodicStream(5000)
                .handler(h -> vertx.eventBus().publish("stats", getStateEntries()));
    }

    private JsonArray getStateEntries() {
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
                    .put("dccstate", latestDccState)
                    .put("notices", state.getNotices());
            bots.add(bot);
        });

        return bots;
    }

}
