package com.lxbluem.state;

import io.vertx.core.json.JsonObject;
import lombok.*;

import java.util.List;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@ToString
class State {
    private MovingAverage movingAverage;
    private DccState dccState;
    private List<String> oldBotNames;
    private List<String> messages;
    private long timestamp;
    private long started;
    private JsonObject pack;
    private String filenameOnDisk;
    private long bytesTotal;
    private long bytes;
    private BotState botState;
}
