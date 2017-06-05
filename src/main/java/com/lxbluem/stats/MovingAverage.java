package com.lxbluem.stats;

import lombok.Getter;
import lombok.Setter;

import java.util.LinkedList;
import java.util.List;

@Getter
@Setter
public class MovingAverage {
    private List<Progress> q;
    private int secondsToSave;

    public MovingAverage(int numberSecs) {
        this.secondsToSave = numberSecs;
        q = new LinkedList<>();
    }

    public double average() {
        Progress lastElement = q.get(q.size() - 1);

        Progress reference = q.stream()
                .filter(p -> p.time + secondsToSave * 1000 < lastElement.time)
                .findFirst()
                .orElse(q.get(0));

        long dt = lastElement.time - reference.time;
        double avg = (dt == 0) ? lastElement.size : 1d * (lastElement.size - reference.size) / dt;

        return avg;
    }

    public void addValue(Progress val) {
        q.add(val);
    }

}
