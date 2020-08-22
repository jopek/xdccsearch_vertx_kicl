package com.lxbluem.state;

import com.lxbluem.AbstractRouteVerticle;
import com.lxbluem.domain.Pack;
import com.lxbluem.model.SerializedRequest;
import com.lxbluem.state.domain.model.State;
import com.lxbluem.state.domain.model.request.*;
import io.vertx.core.Promise;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.rxjava.core.eventbus.Message;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import rx.functions.Action1;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import static com.lxbluem.Addresses.*;
import static io.vertx.core.http.HttpMethod.DELETE;
import static io.vertx.core.http.HttpMethod.GET;

public class StateVerticle extends AbstractRouteVerticle {
    private static final Logger LOG = LoggerFactory.getLogger(StateVerticle.class);

    static final int AVG_SIZE_SEC = 5;

    private final StateService service;

    public StateVerticle(StateService service) {
        this.service = service;
    }

    @Override
    public void start() throws Exception {
        promisedRegisterRouteWithHandler(DELETE, "/state", this::clearFinished);
        promisedRegisterRouteWithHandler(GET, "/state", this::getState);

        handle(BOT_INIT, this::init);
        handle(BOT_NOTICE, this::notice);
        handle(BOT_UPDATE_NICK, this::renameBot);
        handle(BOT_EXIT, this::exit);
        handle(BOT_FAIL, this::fail);
        handle(BOT_DCC_START, this::dccStart);
        handle(BOT_DCC_PROGRESS, this::dccProgress);
        handle(BOT_DCC_FINISH, this::dccFinish);
    }

    private void clearFinished(SerializedRequest serializedRequest, Promise<JsonObject> result) {
        List<String> bots = service.clearFinished();
        vertx.eventBus().publish(REMOVED_STALE_BOTS, new JsonArray(bots));
        result.complete(new JsonObject().put(REMOVED_STALE_BOTS, bots));
    }

    private void getState(SerializedRequest serializedRequest, Promise<JsonObject> result) {
        JsonObject entries = getStateEntriesJsonObject();
        LOG.debug("getState {}", entries);
        result.complete(entries);
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
        Pack pack = body.getJsonObject("pack").mapTo(Pack.class);
        long timestamp = body.getLong("timestamp");

        service.init(new InitRequest(bot, timestamp, pack));

        Map<String, State> state = service.getState();
        JsonObject result = JsonObject.mapFrom(state);
        vertx.eventBus().publish(STATE, result);
    }

    private void notice(Message<JsonObject> eventMessage) {
        JsonObject body = eventMessage.body();
        String bot = body.getString("bot");
        String message = body.getString("message");
        long timestamp = body.getLong("timestamp");
        service.noticeMessage(new NoticeMessageRequest(bot, message, timestamp));
    }

    private void renameBot(Message<JsonObject> eventMessage) {
        JsonObject body = eventMessage.body();
        String bot = body.getString("bot");
        String newBot = body.getString("renameto");
        String message = body.getString("message");
        long timestamp = body.getLong("timestamp");
        service.renameBot(new RenameBotRequest(bot, newBot, message, timestamp));
    }

    private void exit(Message<JsonObject> eventMessage) {
        JsonObject body = eventMessage.body();
        String bot = body.getString("bot");
        String message = body.getString("message");
        long timestamp = body.getLong("timestamp");
        service.exit(new ExitRequest(bot, message, timestamp));
    }

    private void dccStart(Message<JsonObject> eventMessage) {
        JsonObject body = eventMessage.body();
        String bot = body.getString("bot");
        String filenameOnDisk = body.getString("filenameOnDisk");
        long timestamp = body.getLong("timestamp");
        long bytesTotal = body.getLong("bytesTotal");
        service.dccStart(new DccStartRequest(bot, bytesTotal, filenameOnDisk, timestamp));
    }

    private void dccProgress(Message<JsonObject> eventMessage) {
        JsonObject body = eventMessage.body();
        long bytes = body.getLong("bytes", 0L);
        String bot = body.getString("bot");
        long timestamp = body.getLong("timestamp", 1L);
        service.dccProgress(new DccProgressRequest(bot, bytes, timestamp));
    }

    private void dccFinish(Message<JsonObject> eventMessage) {
        JsonObject body = eventMessage.body();
        String bot = body.getString("bot");
        long timestamp = body.getLong("timestamp");
        service.dccFinish(new DccFinishRequest(bot, timestamp));
    }

    private void fail(Message<JsonObject> eventMessage) {
        JsonObject body = eventMessage.body();
        String bot = body.getString("bot");
        long timestamp = body.getLong("timestamp");
        service.fail(new FailRequest(bot, "", timestamp));
    }

    private JsonObject getStateEntriesJsonObject() {
        JsonObject bots = new JsonObject();

        service.getState().forEach((botname, state) -> {
            bots.put(botname, new JsonObject()
                    .put("started", state.getStartedTimestamp())
                    .put("duration", state.getEndedTimestamp() > 0
                            ? state.getEndedTimestamp() - state.getStartedTimestamp()
                            : Instant.now().toEpochMilli() - state.getStartedTimestamp()
                    )
                    .put("timestamp", state.getTimestamp())
                    .put("speed", state.getMovingAverage().average())
                    .put("dccstate", state.getDccState())
                    .put("botstate", state.getBotState())
                    .put("messages", state.getMessages())
                    .put("oldBotNames", state.getOldBotNames())
                    .put("bot", botname)
                    .put("filenameOnDisk", state.getFilenameOnDisk())
                    .put("bytesTotal", state.getBytesTotal())
                    .put("bytes", state.getBytes())
                    .put("pack", JsonObject.mapFrom(state.getPack())));
        });

        return bots;
    }

}
