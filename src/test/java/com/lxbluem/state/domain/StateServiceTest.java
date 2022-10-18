package com.lxbluem.state.domain;

import com.lxbluem.common.domain.Pack;
import com.lxbluem.state.adapters.InMemoryStateRepository;
import com.lxbluem.state.domain.model.BotState;
import com.lxbluem.state.domain.model.DccState;
import com.lxbluem.state.domain.model.MovingAverage;
import com.lxbluem.state.domain.model.State;
import com.lxbluem.state.domain.model.request.DccFinishRequest;
import com.lxbluem.state.domain.model.request.DccProgressRequest;
import com.lxbluem.state.domain.model.request.DccStartRequest;
import com.lxbluem.state.domain.model.request.ExitRequest;
import com.lxbluem.state.domain.model.request.FailRequest;
import com.lxbluem.state.domain.model.request.InitRequest;
import com.lxbluem.state.domain.model.request.NoticeMessageRequest;
import com.lxbluem.state.domain.model.request.RenameBotRequest;
import com.lxbluem.state.domain.ports.StateRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import static com.lxbluem.state.domain.StateService.AVG_SIZE_SEC;
import static com.lxbluem.state.domain.model.DccState.FAIL;
import static com.lxbluem.state.domain.model.DccState.INIT;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class StateServiceTest {

    private StateService ut;
    private StateRepository stateRepository;
    private final Instant fixedInstant = Instant.parse("2020-08-10T10:11:22Z");
    private final Clock clock = Clock.fixed(fixedInstant, ZoneId.systemDefault());

    @BeforeEach
    void setUp() {
        stateRepository = new InMemoryStateRepository();
        ut = new StateService(stateRepository, clock);
    }

    @Test
    void initialize_state() {
        String botName = "Andy";
        Pack pack = Pack.builder()
                .channelName("#someChannel")
                .networkName("someNetwork")
                .serverHostName("someHost")
                .nickName("someRemoteBot")
                .build();
        InitRequest initRequest = new InitRequest(botName, 9999L, pack);

        ut.init(initRequest);

        State stateFromRepo = stateRepository.getStateByBotName(botName);

        State expected = initialStateWithPack(botName, pack);
        expected.setTimestamp(9999L);
        assertEquals(expected, stateFromRepo);
    }

    private State initialStateWithPack(String botName, Pack pack) {
        return State.builder()
                .movingAverage(new MovingAverage(AVG_SIZE_SEC))
                .dccState(INIT)
                .botState(BotState.RUN)
                .oldBotNames(new ArrayList<>())
                .messages(new ArrayList<>())
                .startedTimestamp(Instant.now(clock).toEpochMilli())
                .pack(pack)
                .botName(botName)
                .filenameOnDisk("")
                .build();
    }

    @Test
    void notice_messages_added_to_state() {
        String botName = "Andy";
        State state = initialStateWithPack(botName, null);
        stateRepository.saveStateByBotName(botName, state);

        NoticeMessageRequest noticeMessageRequest = new NoticeMessageRequest(botName, "message1", 9999L);
        ut.noticeMessage(noticeMessageRequest);

        State stateFromRepo;
        stateFromRepo = stateRepository.getStateByBotName(botName);
        assertEquals(Arrays.asList("message1"), stateFromRepo.getMessages());
        assertEquals(9999L, stateFromRepo.getTimestamp());

        NoticeMessageRequest noticeMessageRequest2 = new NoticeMessageRequest(botName, "message2", 10_000L);
        ut.noticeMessage(noticeMessageRequest2);
        stateFromRepo = stateRepository.getStateByBotName(botName);
        assertEquals(Arrays.asList("message1", "message2"), stateFromRepo.getMessages());
        assertEquals(10000L, stateFromRepo.getTimestamp());
    }

    @Test
    void rename_bot_renames_bot_and_holds_name_history() {
        State state = initialStateWithPack("Andy", null);
        stateRepository.saveStateByBotName("Andy", state);

        RenameBotRequest renameBotRequest;

        renameBotRequest = new RenameBotRequest("Andy", "Karl", "bot Andy renamed to Karl", 9999L);
        ut.renameBot(renameBotRequest);

        State stateFromRepo;
        stateFromRepo = stateRepository.getStateByBotName("Karl");
        assertEquals(Arrays.asList("bot Andy renamed to Karl"), stateFromRepo.getMessages());
        assertEquals(9999L, stateFromRepo.getTimestamp());
        assertEquals(Arrays.asList("Andy"), stateFromRepo.getOldBotNames());

        renameBotRequest = new RenameBotRequest("Karl", "Mandy", "bot Karl renamed to Mandy", 10_000L);
        ut.renameBot(renameBotRequest);

        stateFromRepo = stateRepository.getStateByBotName("Mandy");
        assertEquals(Arrays.asList("bot Andy renamed to Karl", "bot Karl renamed to Mandy"), stateFromRepo.getMessages());
        assertEquals(10_000L, stateFromRepo.getTimestamp());
        assertEquals(Arrays.asList("Andy", "Karl"), stateFromRepo.getOldBotNames());
    }

    @Test
    void rename_bot_creates_state_when_no_old_state() {
        RenameBotRequest renameBotRequest;

        renameBotRequest = new RenameBotRequest("Andy", "Karl", "bot Andy renamed to Karl", 9999L);
        ut.renameBot(renameBotRequest);

        State stateFromRepo;
        stateFromRepo = stateRepository.getStateByBotName("Karl");
        assertEquals(Arrays.asList("bot Andy renamed to Karl"), stateFromRepo.getMessages());
        assertEquals(9999L, stateFromRepo.getTimestamp());
        assertEquals(Arrays.asList("Andy"), stateFromRepo.getOldBotNames());
        assertNull(stateFromRepo.getPack());
    }

    @Test
    void exit() {
        ExitRequest exitRequest = new ExitRequest("Andy", "bot Andy exited", 9999L);

        stateRepository.saveStateByBotName("Andy", initialStateWithPack("Andy", null));

        ut.exit(exitRequest);

        State stateFromRepo;
        stateFromRepo = stateRepository.getStateByBotName("Andy");

        assertEquals(Arrays.asList("bot Andy exited"), stateFromRepo.getMessages());
        assertEquals(9999L, stateFromRepo.getTimestamp());
        assertEquals(BotState.EXIT, stateFromRepo.getBotState());
    }

    @Test
    void dcc_start() {
        DccStartRequest dccStartRequest = new DccStartRequest("Andy", 1233123L, "filenameOn.disk", 9999L);

        stateRepository.saveStateByBotName("Andy", initialStateWithPack("Andy", null));

        ut.dccStart(dccStartRequest);

        State stateFromRepo;
        stateFromRepo = stateRepository.getStateByBotName("Andy");

        assertEquals(9999L, stateFromRepo.getTimestamp());
        assertEquals(DccState.START, stateFromRepo.getDccState());
        assertEquals("filenameOn.disk", stateFromRepo.getFilenameOnDisk());
        assertEquals(1233123L, stateFromRepo.getBytesTotal());
    }

    @Test
    void dcc_progress() {
        DccProgressRequest dccProgressRequest = new DccProgressRequest("Andy", 5555L, 9999L);

        State initialState = initialStateWithPack("Andy", null);
        assertTrue(initialState.getMovingAverage().getQ().isEmpty());
        stateRepository.saveStateByBotName("Andy", initialState);

        ut.dccProgress(dccProgressRequest);

        State stateFromRepo;
        stateFromRepo = stateRepository.getStateByBotName("Andy");

        assertEquals(9999L, stateFromRepo.getTimestamp());
        assertEquals(DccState.PROGRESS, stateFromRepo.getDccState());
        assertEquals(5555L, stateFromRepo.getBytes());

        assertEquals(1, stateFromRepo.getMovingAverage().getQ().size());
    }

    @Test
    void dcc_finish() {
        DccFinishRequest dccFinishRequest = new DccFinishRequest("Andy", 9999L);

        State initialState = initialStateWithPack("Andy", null);
        stateRepository.saveStateByBotName("Andy", initialState);

        ut.dccFinish(dccFinishRequest);

        State stateFromRepo;
        stateFromRepo = stateRepository.getStateByBotName("Andy");

        assertEquals(9999L, stateFromRepo.getTimestamp());
        assertEquals(DccState.FINISH, stateFromRepo.getDccState());
    }

    @Test
    void fail() {
        FailRequest failRequest = new FailRequest("Andy", "failed because reasons.", 9999L);

        stateRepository.saveStateByBotName("Andy", initialStateWithPack("Andy", null));

        ut.fail(failRequest);

        State stateFromRepo;
        stateFromRepo = stateRepository.getStateByBotName("Andy");

        assertEquals(9999L, stateFromRepo.getTimestamp());
        assertEquals(DccState.FAIL, stateFromRepo.getDccState());
        assertEquals(Arrays.asList("failed because reasons."), stateFromRepo.getMessages());
    }

    @Test
    void get_state_entries() {

        stateRepository.saveStateByBotName("Andy", initialStateWithPack("Andy", null));
        stateRepository.saveStateByBotName("Karl", initialStateWithPack("Karl", null));
        stateRepository.saveStateByBotName("Mandy", initialStateWithPack("Mandy", null));

        AtomicReference<Map<String, State>> stateMapRef = new AtomicReference<>();
        ut.getState(stateMapRef::set);
        Map<String, State> stateMap = stateMapRef.get();

        State stateFromRepoAndy = stateRepository.getStateByBotName("Andy");
        State stateFromRepoKarl = stateRepository.getStateByBotName("Karl");
        State stateFromRepoMandy = stateRepository.getStateByBotName("Mandy");

        assertEquals(3, stateMap.size());
        assertEquals(stateFromRepoAndy, stateMap.get("Andy"));
        assertEquals(stateFromRepoKarl, stateMap.get("Karl"));
        assertEquals(stateFromRepoMandy, stateMap.get("Mandy"));
    }

    @Test
    void clear_finished() {
        State stateAndy = initialStateWithPack("Andy", null);
        stateAndy.setDccState(DccState.INIT);
        stateRepository.saveStateByBotName("Andy", stateAndy);
        State stateKarl = initialStateWithPack("Karl", null);
        stateKarl.setDccState(DccState.PROGRESS);
        stateRepository.saveStateByBotName("Karl", stateKarl);
        State stateMandy = initialStateWithPack("Mandy", null);
        stateMandy.setDccState(DccState.FINISH);
        stateRepository.saveStateByBotName("Mandy", stateMandy);
        State stateSusy = initialStateWithPack("Susy", null);
        stateSusy.setDccState(FAIL);
        stateRepository.saveStateByBotName("Susy", stateSusy);

        AtomicReference<List<String>> ref = new AtomicReference<>();
        ut.clearFinished(ref::set);
        List<String> removedBotNames = ref.get();

        assertEquals(Arrays.asList("Mandy", "Susy"), removedBotNames);

        State stateFromRepoAndy = stateRepository.getStateByBotName("Andy");
        State stateFromRepoKarl = stateRepository.getStateByBotName("Karl");
        State stateFromRepoMandy = stateRepository.getStateByBotName("Mandy");
        State stateFromRepoSusy = stateRepository.getStateByBotName("Susy");

        Map<String, State> stateMap = stateRepository.getStateEntries();
        assertEquals(2, stateMap.size());

        assertEquals(stateFromRepoAndy, stateMap.get("Andy"));
        assertEquals(stateFromRepoKarl, stateMap.get("Karl"));
        assertEquals(null, stateMap.get("Mandy"));
        assertEquals(null, stateMap.get("Susy"));
    }
}
