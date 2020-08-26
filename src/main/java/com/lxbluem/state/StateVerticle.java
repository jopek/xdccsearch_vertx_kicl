package com.lxbluem.state;

import com.lxbluem.common.domain.Pack;
import com.lxbluem.common.infrastructure.AbstractRouteVerticle;
import com.lxbluem.common.infrastructure.Address;
import com.lxbluem.common.infrastructure.SerializedRequest;
import com.lxbluem.state.domain.StateService;
import com.lxbluem.state.domain.model.State;
import com.lxbluem.state.domain.model.request.*;
import io.vertx.core.Promise;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.rxjava.core.eventbus.Message;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import rx.functions.Action1;

import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

import static com.lxbluem.common.infrastructure.Address.*;
import static io.vertx.core.http.HttpMethod.DELETE;
import static io.vertx.core.http.HttpMethod.GET;

public class StateVerticle extends AbstractRouteVerticle {
    private static final Logger LOG = LoggerFactory.getLogger(StateVerticle.class);

    private final StateService service;
    private final Clock clock;

    public StateVerticle(StateService service, Clock clock) {
        this.service = service;
        this.clock = clock;
    }

    @Override
    public void start() {
        promisedRegisterRouteWithHandler(DELETE, "/state", this::clearFinished);
        promisedRegisterRouteWithHandler(GET, "/state", this::getState);

        handle(BOT_INITIALIZED, this::init);
        handle(BOT_NOTICE, this::notice);
        handle(BOT_NICK_UPDATED, this::renameBot);
        handle(BOT_EXITED, this::exit);
        handle(BOT_FAILED, this::fail);
        handle(DCC_STARTED, this::dccStart);
        handle(DCC_PROGRESSED, this::dccProgress);
        handle(DCC_FINISHED, this::dccFinish);
    }

    private void clearFinished(SerializedRequest serializedRequest, Promise<JsonObject> result) {
        RemovedBotNamesPresenter presenter = new RemovedBotNamesPresenter();
        service.clearFinished(presenter);
        vertx.eventBus().publish(REMOVED_STALE_BOTS.address(), presenter.getBotList());
        result.complete(new JsonObject().put(REMOVED_STALE_BOTS.address(), presenter.getBotList()));
    }

    private void getState(SerializedRequest serializedRequest, Promise<JsonObject> result) {
        StatePresenter presenter = new StatePresenter(clock);
        service.getState(presenter);
        JsonObject entries = presenter.getStateDto();
        LOG.debug("getState {}", entries);
        result.complete(entries);
    }

    private void handle(Address address, Action1<Message<JsonObject>> method) {
        vertx.eventBus()
                .<JsonObject>consumer(address.address())
                .toObservable()
                .subscribe(method);
    }

    private void init(Message<JsonObject> eventMessage) {
        JsonObject body = eventMessage.body();
        String bot = body.getString("bot");
        Pack pack = body.getJsonObject("pack").mapTo(Pack.class);
        long timestamp = body.getLong("timestamp");

        service.init(new InitRequest(bot, timestamp, pack));

        StatePresenter presenter = new StatePresenter(clock);
        service.getState(presenter);
        JsonObject entries = presenter.getStateDto();
        vertx.eventBus().publish(STATE.address(), entries);
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

    private static class StatePresenter implements Consumer<Map<String, State>> {
        private final JsonObject bots = new JsonObject();
        private final Clock clock;

        private StatePresenter(Clock clock) {
            this.clock = clock;
        }

        public JsonObject getStateDto() {
            return bots;
        }

        @Override
        public void accept(Map<String, State> stateMap) {
            stateMap.forEach((botname, state) -> bots.put(botname, new JsonObject()
                    .put("started", state.getStartedTimestamp())
                    .put("duration", state.getEndedTimestamp() > 0
                            ? state.getEndedTimestamp() - state.getStartedTimestamp()
                            : Instant.now(clock).toEpochMilli() - state.getStartedTimestamp()
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
                    .put("pack", JsonObject.mapFrom(state.getPack()))));
        }
    }

    private static class RemovedBotNamesPresenter implements Consumer<List<String>> {
        private JsonArray bots = new JsonArray();

        public JsonArray getBotList() {
            return bots;
        }

        @Override
        public void accept(List<String> botList) {
            this.bots = new JsonArray(botList);
        }
    }
}
