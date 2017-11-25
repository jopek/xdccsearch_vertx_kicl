package com.lxbluem.state;

import com.lxbluem.AbstractRouteVerticle;
import com.lxbluem.model.SerializedRequest;
import io.vertx.core.Future;
import io.vertx.core.json.JsonObject;
import io.vertx.rxjava.core.eventbus.Message;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import rx.functions.Action1;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static com.lxbluem.Addresses.*;
import static com.lxbluem.state.DccState.*;
import static io.vertx.core.http.HttpMethod.GET;
import static java.util.stream.Collectors.toMap;

public class StateVerticle extends AbstractRouteVerticle {
    private static Logger LOG = LoggerFactory.getLogger(StateVerticle.class);
    private Map<String, State> stateMap = new HashMap<>();
    private Map<String, String> aliasMap = new HashMap<>();

    private static final int AVG_SIZE_SEC = 5;


    @Override
    public void start() throws Exception {
        registerRouteWithHandler(GET, "/state", this::getState);
        registerRouteWithHandler(GET, "/state/:botname", this::getStateByBotName);

        handle(BOT_INIT, this::init);
        handle(BOT_NOTICE, this::notice);
        handle(BOT_UPDATE_NICK, this::renameBot);
        handle(BOT_EXIT, this::exit);
        handle(BOT_FAIL, this::fail);
        handle(BOT_DCC_START, this::dccStart);
        handle(BOT_DCC_PROGRESS, this::dccProgress);
        handle(BOT_DCC_FINISH, this::dccFinish);

        setupStatePublishInterval();
    }

    private void wrapMessage(Message<JsonObject> event) {
        final JsonObject pack = event.body().getJsonObject("pack");
        String botname = pack.getString("botname");
        JsonObject state = getStateByBotName(botname);
        JsonObject toBePublished = new JsonObject().put("topic", event.address());

        vertx.eventBus().publish(STATE, toBePublished.mergeIn(state));
    }

    private void getStateByBotName(SerializedRequest serializedRequest, Future<JsonObject> jsonObjectFuture) {
        String botnamePathParam = serializedRequest.getParams().get("botname");
        if (botnamePathParam == null || botnamePathParam.isEmpty()) {
            jsonObjectFuture.fail("pack id format");
            return;
        }

        JsonObject entry = getStateByBotName(botnamePathParam);

        if (!entry.isEmpty())
            jsonObjectFuture.complete(entry);
        else
            jsonObjectFuture.fail("not found");
    }

    private JsonObject getStateByBotName(String requestedBotname) {
        final String aliasBotName = aliasMap.get(requestedBotname);

        final JsonObject stateEntries = getStateEntries();

        JsonObject stateJsonObject = stateEntries.getJsonObject(requestedBotname);

        if (stateJsonObject == null && aliasBotName != null)
            stateJsonObject = stateEntries.getJsonObject(aliasBotName);

        if (stateJsonObject == null)
            return new JsonObject();

        return stateJsonObject;
    }

    private void getState(SerializedRequest serializedRequest, Future<JsonObject> jsonObjectFuture) {
        jsonObjectFuture.complete(getStateEntries());
    }

    private void handle(String address, Action1<Message<JsonObject>> method) {
        vertx.eventBus()
                .<JsonObject>consumer(address)
                .toObservable()
                .subscribe(method);
    }

    private void init(Message<JsonObject> eventMessage) {
        JsonObject body = eventMessage.body();
        String bot = body.getString("bot");
        JsonObject pack = body.getJsonObject("pack");
        long timestamp = body.getLong("timestamp");

        stateMap.putIfAbsent(bot, getInitialState());

        final State state = stateMap.get(bot);
        state.setTimestamp(timestamp);
    }

    private State getInitialState() {
        return State.builder()
                .movingAverage(new MovingAverage(AVG_SIZE_SEC))
                .dccState(INIT)
                .notices(new ArrayList<>())
                .messages(new ArrayList<>())
                .started(Instant.now().toEpochMilli())
                .build();
    }

    private void notice(Message<JsonObject> eventMessage) {
        JsonObject body = eventMessage.body();
        String bot = body.getString("bot");
        String message = body.getString("message");
        long timestamp = body.getLong("timestamp");

        State state = updateState(bot, timestamp);

        state.getMessages().add(message);
    }

    private void renameBot(Message<JsonObject> eventMessage) {
        JsonObject body = eventMessage.body();
        String bot = body.getString("bot");
        String newBot = body.getString("renameto");
        String message = body.getString("message");
        long timestamp = body.getLong("timestamp");

        State state = stateMap.remove(bot);
        if (state == null)
            state = getInitialState();
        stateMap.put(newBot, state);
        aliasMap.put(bot, newBot);

        state.setTimestamp(timestamp);
        state.getMessages().add(message);
    }

    private void exit(Message<JsonObject> eventMessage) {
        JsonObject body = eventMessage.body();
        String bot = body.getString("bot");
        String message = body.getString("message");
        long timestamp = body.getLong("timestamp");

        State state = updateState(bot, timestamp);
        state.getMessages().add(message);
    }

    private void dccStart(Message<JsonObject> eventMessage) {
        JsonObject body = eventMessage.body();
        String bot = body.getString("bot");
        String filenameOnDisk = body.getString("filenameOnDisk");
        long timestamp = body.getLong("timestamp");
        long bytesTotal = body.getLong("bytesTotal");

        State state = updateState(bot, timestamp);
        state.setDccState(START);
        state.setBytesTotal(bytesTotal);
        state.setFilenameOnDisk(filenameOnDisk);
    }

    private void dccProgress(Message<JsonObject> eventMessage) {
        JsonObject body = eventMessage.body();
        long bytes = body.getLong("bytes", 0L);
        String bot = body.getString("bot");
        long timestamp = body.getLong("timestamp", 1L);

        State state = updateState(bot, timestamp);

        MovingAverage movingAverage = state.getMovingAverage();
        movingAverage.addValue(new Progress(bytes, timestamp));

        state.setDccState(PROGRESS);
        state.setBytes(bytes);
    }

    private void dccFinish(Message<JsonObject> eventMessage) {
        JsonObject body = eventMessage.body();
        String bot = body.getString("bot");
        long timestamp = body.getLong("timestamp");

        State state = updateState(bot, timestamp);
        state.setDccState(FINISH);
    }

    private void fail(Message<JsonObject> eventMessage) {
        JsonObject body = eventMessage.body();
        String bot = body.getString("bot");
        long timestamp = body.getLong("timestamp");

        State state = updateState(bot, timestamp);
        state.setDccState(FAIL);
        state.getMessages().add(body.getString("message"));
    }

    private State updateState(String botname, long timestamp) {
        final State state = stateMap.get(botname);
        state.setTimestamp(timestamp);
        return state;
    }

    private void setupStatePublishInterval() {
        vertx.periodicStream(5000)
                .handler(h -> {
                    final Map<String, Object> collect = getStateEntries()
                            .getMap()
                            .entrySet()
                            .stream()
                            .filter(stringObjectEntry -> ((JsonObject) stringObjectEntry.getValue())
                                    .getString("dccstate", "")
                                    .equalsIgnoreCase("PROGRESS")
                            )
                            .collect(toMap(Map.Entry::getKey, Map.Entry::getValue));

                    if (collect.size() == 0)
                        return;

                    final JsonObject stateEntriesFiltered = new JsonObject();
                    stateEntriesFiltered.getMap().putAll(collect);

                    vertx.eventBus().publish("stats", stateEntriesFiltered);
                });
    }

    private JsonObject getStateEntries() {
        JsonObject bots = new JsonObject();

        stateMap.forEach((botname, state) -> {
            if (state == null) {
                LOG.info("stateMap {}", stateMap);
                return;
            }

            bots.put(botname, new JsonObject()
                    .put("startedTimestamp", state.getStarted())
                    .put("duration", state.getTimestamp() - state.getStarted())
                    .put("timestamp", state.getTimestamp())
                    .put("speed", state.getMovingAverage().average())
                    .put("dccstate", state.getDccState())
                    .put("messages", state.getMessages())
                    .put("bot", botname)
                    .put("filenameOnDisk", state.getFilenameOnDisk())
                    .put("bytesTotal", state.getBytesTotal())
                    .put("bytes", state.getBytes())
                    .put("bot", botname)
                    .put("pack", state.getPack()));
        });

        return bots;
    }

}
