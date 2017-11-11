package com.lxbluem.state;

import com.lxbluem.AbstractRouteVerticle;
import com.lxbluem.model.SerializedRequest;
import io.vertx.core.Future;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.rxjava.core.eventbus.Message;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import rx.functions.Action1;

import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

import static com.lxbluem.Addresses.*;
import static com.lxbluem.state.DccState.*;
import static io.vertx.core.http.HttpMethod.GET;

public class StateVerticle extends AbstractRouteVerticle {
    private static Logger LOG = LoggerFactory.getLogger(StateVerticle.class);
    private Map<JsonObject, State> stateMap = new HashMap<>();

    private static final int AVG_SIZE_SEC = 5;


    @Override
    public void start() throws Exception {
        registerRouteWithHandler(GET, "/state", this::getState);
        registerRouteWithHandler(GET, "/state/:packid", this::getStateByPackId);

        handle(BOT_INIT, this::init);
        handle(BOT_NOTICE, this::notice);
        handle(BOT_EXIT, this::exit);
        handle(BOT_FAIL, this::fail);
        handle(BOT_DCC_START, this::dccStart);
        handle(BOT_DCC_PROGRESS, this::dccProgress);
        handle(BOT_DCC_FINISH, this::dccFinish);

        handle(BOT_INIT, this::wrapMessage);
        handle(BOT_NOTICE, this::wrapMessage);
        handle(BOT_EXIT, this::wrapMessage);
        handle(BOT_FAIL, this::wrapMessage);
        handle(BOT_DCC_START, this::wrapMessage);
        handle(BOT_DCC_PROGRESS, this::wrapMessage);
        handle(BOT_DCC_FINISH, this::wrapMessage);

//        setupStatePublishInterval();
    }

    private void wrapMessage(Message<JsonObject> event) {
        final JsonObject pack = event.body().getJsonObject("pack");
        long pid = pack.getLong("pid");

        Optional<Object> state = getStateByPackId(pid);

        JsonObject toBePublished = new JsonObject().put("topic", event.address());

        state.map(stateJsonObject -> (JsonObject) stateJsonObject)
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
        state.getDccStates().add(INIT);
    }

    private void notice(Message<JsonObject> eventMessage) {
        JsonObject body = eventMessage.body();
        JsonObject pack = body.getJsonObject("pack");
        String message = body.getString("message");
        long timestamp = body.getLong("timestamp");

        State state = updateVerticleState(pack, timestamp);

        state.getNotices().add(message);
    }

    private void exit(Message<JsonObject> eventMessage) {
        JsonObject body = eventMessage.body();
        JsonObject pack = body.getJsonObject("pack");
        long timestamp = body.getLong("timestamp");

        State state = updateVerticleState(pack, timestamp);
        state.getMessages().add(body.getString("message"));
    }

    private void dccStart(Message<JsonObject> eventMessage) {
        JsonObject body = eventMessage.body();
        JsonObject pack = body.getJsonObject("pack");
        long timestamp = body.getLong("timestamp");

        State state = updateVerticleState(pack, timestamp);
        state.getDccStates().add(START);
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
        dccStates.add(PROGRESS);
        state.setDccStates(dccStates);
    }

    private void dccFinish(Message<JsonObject> eventMessage) {
        JsonObject body = eventMessage.body();
        JsonObject pack = body.getJsonObject("pack");
        long timestamp = body.getLong("timestamp");

        State state = updateVerticleState(pack, timestamp);
        state.getDccStates().add(FINISH);
    }

    private void fail(Message<JsonObject> eventMessage) {
        JsonObject body = eventMessage.body();
        JsonObject pack = body.getJsonObject("pack");
        long timestamp = body.getLong("timestamp");

        State state = updateVerticleState(pack, timestamp);
        state.getDccStates().add(FAIL);
        state.getMessages().add(body.getString("message"));
    }

    private State updateVerticleState(JsonObject pack, long timestamp) {
        stateMap.putIfAbsent(pack, State.builder()
                .movingAverage(new MovingAverage(AVG_SIZE_SEC))
                .dccStates(new ArrayList<>())
                .notices(new ArrayList<>())
                .messages(new ArrayList<>())
                .started(Instant.now().toEpochMilli())
                .build());

        final State state = stateMap.get(pack);
        state.setTimestamp(timestamp);
        return state;
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
                    .put("started", state.getStarted())
                    .put("duration", state.getTimestamp() - state.getStarted())
                    .put("timestamp", state.getTimestamp())
                    .put("speed", state.getMovingAverage().average())
                    .put("dccstate", latestDccState)
                    .put("messages", state.getMessages())
                    .put("notices", state.getNotices())
                    .put("pack", pack);
            bots.add(bot);
        });

        return bots;
    }

}
