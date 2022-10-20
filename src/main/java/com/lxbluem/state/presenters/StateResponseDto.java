package com.lxbluem.state.presenters;

import lombok.Builder;
import lombok.Data;
import lombok.Getter;

import java.util.List;
import java.util.Map;

@Getter
//@Setter
//@Builder
@Data
public class StateResponseDto {
    private final Map<String, StateResponseDto.StateEntry> map;

    @Data
    @Builder
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











