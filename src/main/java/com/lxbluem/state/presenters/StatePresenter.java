package com.lxbluem.state.presenters;

import com.lxbluem.state.domain.model.State;
import io.vertx.core.json.JsonObject;

import java.time.Clock;
import java.time.Instant;
import java.util.Map;
import java.util.function.Consumer;

public class StatePresenter implements Consumer<Map<String, State>> {
    private final JsonObject bots = new JsonObject();
    private final Clock clock;

    public StatePresenter(Clock clock) {
        this.clock = clock;
    }

    public JsonObject getStateDto() {
        return bots;
    }

    @Override
    public void accept(Map<String, State> stateMap) {
        stateMap.forEach((botname, state) -> bots.put(botname, new JsonObject()
                .put("started", state.getStartedTimestamp())
                .put("duration", getDuration(state))
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

    private long getDuration(State state) {
        if (state.getEndedTimestamp() > 0)
            return state.getEndedTimestamp() - state.getStartedTimestamp();

        return Instant.now(clock).toEpochMilli() - state.getStartedTimestamp();
    }
}
