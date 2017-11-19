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
    private List<DccState> dccStates;
    private List<String> notices;
    private List<String> messages;
    private long timestamp;
    private long started;
    private JsonObject pack;
}