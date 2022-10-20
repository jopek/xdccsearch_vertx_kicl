package com.lxbluem.state;

import com.lxbluem.common.domain.events.BotExitedEvent;
import com.lxbluem.common.domain.events.BotFailedEvent;
import com.lxbluem.common.domain.events.BotInitializedEvent;
import com.lxbluem.common.domain.events.BotNoticeEvent;
import com.lxbluem.common.domain.events.BotRenamedEvent;
import com.lxbluem.common.infrastructure.AbstractRouteVerticle;
import com.lxbluem.common.infrastructure.Address;
import com.lxbluem.common.infrastructure.SerializedRequest;
import com.lxbluem.state.domain.StateService;
import com.lxbluem.state.domain.model.request.DccFinishRequest;
import com.lxbluem.state.domain.model.request.DccProgressRequest;
import com.lxbluem.state.domain.model.request.DccStartRequest;
import com.lxbluem.state.domain.model.request.ExitRequest;
import com.lxbluem.state.domain.model.request.FailRequest;
import com.lxbluem.state.domain.model.request.InitRequest;
import com.lxbluem.state.domain.model.request.NoticeMessageRequest;
import com.lxbluem.state.domain.model.request.RenameBotRequest;
import com.lxbluem.state.presenters.StatePresenter;
import io.vertx.core.CompositeFuture;
import io.vertx.core.Promise;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.rxjava.core.eventbus.Message;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import rx.functions.Action1;

import java.time.Clock;
import java.util.List;
import java.util.function.Consumer;

import static com.lxbluem.common.infrastructure.Address.*;
import static io.vertx.core.http.HttpMethod.DELETE;
import static io.vertx.core.http.HttpMethod.GET;

public class StateVerticle extends AbstractRouteVerticle {
    private static final Logger LOG = LoggerFactory.getLogger(StateVerticle.class);
    public static final String TIMESTAMP_MSG_KEY = "timestamp";
    public static final String BOT_MSG_KEY = "bot";

    private final StateService service;
    private final Clock clock;

    public StateVerticle(StateService service, Clock clock) {
        this.service = service;
        this.clock = clock;
    }

    @Override
    public void start(Promise<Void> start) {
        CompositeFuture.all(
                        registerRoute(DELETE, "/state", this::clearFinished),
                        registerRoute(GET, "/state", this::getState)
                )
                .onComplete(unused -> start.complete());

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
        BotInitializedEvent event = eventMessage.body().mapTo(BotInitializedEvent.class);
        InitRequest initRequest = new InitRequest(event.getBot(), event.getTimestamp(), event.getPack());

        service.init(initRequest);

        StatePresenter presenter = new StatePresenter(clock);
        service.getState(presenter);
        JsonObject entries = presenter.getStateDto();
        vertx.eventBus().publish(STATE.address(), entries);
    }

    private void notice(Message<JsonObject> eventMessage) {
        BotNoticeEvent event = eventMessage.body().mapTo(BotNoticeEvent.class);
        service.noticeMessage(new NoticeMessageRequest(event.getBot(), event.getMessage(), event.getTimestamp()));
    }

    private void renameBot(Message<JsonObject> eventMessage) {
        BotRenamedEvent event = eventMessage.body().mapTo(BotRenamedEvent.class);
        RenameBotRequest renameBotRequest = new RenameBotRequest(event.getBot(), event.getRenameto(), event.getMessage(), event.getTimestamp());
        service.renameBot(renameBotRequest);
    }

    private void exit(Message<JsonObject> eventMessage) {
        BotExitedEvent event = eventMessage.body().mapTo(BotExitedEvent.class);
        ExitRequest exitRequest = new ExitRequest(event.getBot(), event.getMessage(), event.getTimestamp());
        service.exit(exitRequest);
    }

    private void dccStart(Message<JsonObject> eventMessage) {
        JsonObject body = eventMessage.body();
        String bot = body.getString(BOT_MSG_KEY);
        String filenameOnDisk = body.getString("filenameOnDisk");
        long timestamp = body.getLong(TIMESTAMP_MSG_KEY);
        long bytesTotal = body.getLong("bytesTotal");
        service.dccStart(new DccStartRequest(bot, bytesTotal, filenameOnDisk, timestamp));
    }

    private void dccProgress(Message<JsonObject> eventMessage) {
        JsonObject body = eventMessage.body();
        long bytes = body.getLong("bytes", 0L);
        String bot = body.getString(BOT_MSG_KEY);
        long timestamp = body.getLong(TIMESTAMP_MSG_KEY, 1L);
        service.dccProgress(new DccProgressRequest(bot, bytes, timestamp));
    }

    private void dccFinish(Message<JsonObject> eventMessage) {
        JsonObject body = eventMessage.body();
        String bot = body.getString(BOT_MSG_KEY);
        long timestamp = body.getLong(TIMESTAMP_MSG_KEY);
        service.dccFinish(new DccFinishRequest(bot, timestamp));
    }

    private void fail(Message<JsonObject> eventMessage) {
        BotFailedEvent event = eventMessage.body().mapTo(BotFailedEvent.class);
        service.fail(new FailRequest(event.getBot(), "", event.getTimestamp()));
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
