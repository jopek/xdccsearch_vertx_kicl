package com.lxbluem.state;

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
    private long started;
    private long ended;
}
