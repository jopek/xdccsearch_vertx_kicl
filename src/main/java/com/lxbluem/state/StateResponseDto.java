package com.lxbluem.state;

import com.lxbluem.domain.Pack;
import com.lxbluem.state.domain.model.BotState;
import com.lxbluem.state.domain.model.DccState;
import com.lxbluem.state.domain.model.MovingAverage;
import lombok.Builder;
import lombok.Data;
import lombok.Getter;

import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Getter
public class StateResponseDto  {
    private Map<String, StateResponseDto.StateEntry> map=new HashMap<>();
    @Data @Builder
    static class StateEntry {
        private long started;
        private long duration;
        private long timestamp;
        private double speed;
        private String dccState;
        private String botState;
        private List<String> messages;
        private List<String> oldBotNames;
        private String bot;
        private String filenameOnDisk;
        private long bytesTotal;
        private long bytes;
        private Map<String, String> pack;
    }
}











