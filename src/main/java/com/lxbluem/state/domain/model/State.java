package com.lxbluem.state.domain.model;

import com.lxbluem.domain.Pack;
import lombok.*;

import java.util.List;

@Data
@Builder
@ToString
public class State {
    private MovingAverage movingAverage;
    private DccState dccState;
    private List<String> oldBotNames;
    private List<String> messages;
    private long timestamp;
    private long endedTimestamp;
    private long startedTimestamp;
    private Pack pack;
    private String filenameOnDisk;
    private long bytesTotal;
    private long bytes;
    private BotState botState;
}
