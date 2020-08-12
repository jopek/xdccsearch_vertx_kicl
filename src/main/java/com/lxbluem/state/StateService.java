package com.lxbluem.state;

import com.lxbluem.domain.Pack;
import com.lxbluem.state.domain.model.BotState;
import com.lxbluem.state.domain.model.MovingAverage;
import com.lxbluem.state.domain.model.Progress;
import com.lxbluem.state.domain.model.State;
import com.lxbluem.state.domain.model.request.*;
import com.lxbluem.state.domain.ports.StateRepository;

import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static com.lxbluem.state.domain.model.DccState.*;
import static java.util.stream.Collectors.toList;

public class StateService {

    private final StateRepository stateRepository;
    private final Clock clock;
    static final int AVG_SIZE_SEC = 5;

    public StateService(StateRepository stateRepository, Clock clock) {
        this.stateRepository = stateRepository;
        this.clock = clock;
    }

    public void init(InitRequest initRequest) {
        String botname = initRequest.getBotName();
        Pack pack = initRequest.getPack();
        long timestamp = initRequest.getTimestamp();
        State initState = getInitialState();

        initState.setBotName(botname);
        initState.setPack(pack);
        initState.setTimestamp(timestamp);

        stateRepository.saveStateByBotName(botname, initState);
    }

    private State getInitialState() {
        return State.builder()
                .movingAverage(new MovingAverage(AVG_SIZE_SEC))
                .dccState(INIT)
                .botState(BotState.RUN)
                .oldBotNames(new ArrayList<>())
                .messages(new ArrayList<>())
                .startedTimestamp(Instant.now(clock).toEpochMilli())
                .build();
    }

    public void noticeMessage(NoticeMessageRequest noticeMessageRequest) {
        String botname = noticeMessageRequest.getBotName();
        String message = noticeMessageRequest.getMessage();
        long timestamp = noticeMessageRequest.getTimestamp();

        State state = stateRepository.getStateByBotName(botname);

        state.getMessages().add(message);
        state.setTimestamp(timestamp);

        stateRepository.saveStateByBotName(botname, state);
    }

    public void renameBot(RenameBotRequest renameBotRequest) {
        String botname = renameBotRequest.getBotName();
        String newBotname = renameBotRequest.getNewBotName();
        String message = renameBotRequest.getMessage();
        long timestamp = renameBotRequest.getTimestamp();

        State state = stateRepository.removeStateByBotName(botname);
        if (state == null)
            state = getInitialState();

        state.getMessages().add(message);
        state.getOldBotNames().add(botname);
        state.setTimestamp(timestamp);

        stateRepository.saveStateByBotName(newBotname, state);
    }

    public void exit(ExitRequest exitRequest) {
        String botname = exitRequest.getBotName();
        String message = exitRequest.getMessage();
        long timestamp = exitRequest.getTimestamp();

        State state = stateRepository.getStateByBotName(botname);

        state.getMessages().add(message);
        state.setTimestamp(timestamp);
        state.setBotState(BotState.EXIT);

        stateRepository.saveStateByBotName(botname, state);
    }

    public void dccStart(DccStartRequest dccStartRequest) {
        String botname = dccStartRequest.getBotName();
        String filenameOnDisk = dccStartRequest.getFilenameOnDisk();
        long timestamp = dccStartRequest.getTimestamp();
        long bytesTotal = dccStartRequest.getBytesTotal();

        State state = stateRepository.getStateByBotName(botname);

        state.setDccState(START);
        state.setBytesTotal(bytesTotal);
        state.setFilenameOnDisk(filenameOnDisk);
        state.setTimestamp(timestamp);

        stateRepository.saveStateByBotName(botname, state);
    }

    public void dccProgress(DccProgressRequest dccProgressRequest) {
        String botname = dccProgressRequest.getBotName();
        long timestamp = dccProgressRequest.getTimestamp();
        long bytes = dccProgressRequest.getBytes();

        State state = stateRepository.getStateByBotName(botname);

        state.setDccState(PROGRESS);
        state.setBytes(bytes);
        state.setTimestamp(timestamp);

        MovingAverage movingAverage = state.getMovingAverage();
        movingAverage.addValue(new Progress(bytes, timestamp));

        stateRepository.saveStateByBotName(botname, state);
    }

    public void dccFinish(DccFinishRequest dccProgressRequest) {
        String botname = dccProgressRequest.getBotName();
        long timestamp = dccProgressRequest.getTimestamp();

        State state = stateRepository.getStateByBotName(botname);

        state.setDccState(FINISH);
        state.setTimestamp(timestamp);

        stateRepository.saveStateByBotName(botname, state);
    }

    public void fail(FailRequest dccProgressRequest) {
        String botname = dccProgressRequest.getBotName();
        long timestamp = dccProgressRequest.getTimestamp();
        String message = dccProgressRequest.getMessage();

        State state = stateRepository.getStateByBotName(botname);

        state.getMessages().add(message);
        state.setDccState(FAIL);
        state.setTimestamp(timestamp);

        stateRepository.saveStateByBotName(botname, state);
    }

    public Map<String, State> getState() {
        return stateRepository.getStateEntries();
    }

    public List<String> clearFinished() {
        Map<String, State> stateMap = stateRepository.getStateEntries();
        final List<String> bots = stateMap.entrySet()
                .stream()
                .filter(entry -> entry.getValue() != null)
                .filter(entry ->
                        Arrays.asList(FINISH, FAIL).contains(entry.getValue().getDccState())
                                || entry.getValue().getBotState().equals(BotState.EXIT)
                )
                .map(Map.Entry::getKey)
                .collect(toList());

        bots.forEach(stateRepository::removeStateByBotName);

        return bots;
    }
}
