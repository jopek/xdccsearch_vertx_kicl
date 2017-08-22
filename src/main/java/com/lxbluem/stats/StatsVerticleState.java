package com.lxbluem.stats;

import lombok.*;

import java.util.List;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StatsVerticleState {
    private MovingAverage movingAverage;
    private List<BotState> botstates;
    private List<String> notices;
    private long started;
    private long ended;
}
