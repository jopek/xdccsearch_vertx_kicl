package com.lxbluem.state.domain.model;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;

import java.util.LinkedList;
import java.util.List;

@Getter
@Setter
@EqualsAndHashCode
public class MovingAverage {
    private List<Progress> q;
    private int secondsToSave;

    public MovingAverage(int numberSecs) {
        this.secondsToSave = numberSecs;
        q = new LinkedList<>();
    }

    public double average() {
        int index;
        if (!q.isEmpty())
            index = q.size() - 1;
        else
            return 0;

        Progress lastElement = q.get(index);

        Progress reference = q.stream()
                 .filter(p -> p.time() + secondsToSave * 1000L < lastElement.time())
                .findFirst()
                .orElse(q.get(0));

        long dt = lastElement.time() - reference.time();

        return (dt == 0) ? lastElement.size() : 1d * (lastElement.size() - reference.size()) / dt;
    }

    public void addValue(Progress val) {
        q.add(val);
    }

    @Override
    public String toString() {
        return String.format("%.3f", average());
    }
}
