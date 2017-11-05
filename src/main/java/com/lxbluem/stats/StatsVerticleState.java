package com.lxbluem.stats;

import lombok.*;

import java.util.List;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@ToString
public class StatsVerticleState {
    private MovingAverage movingAverage;
    private List<DccState> dccStates;
    private List<String> notices;
    private long started;
    private long ended;
}
