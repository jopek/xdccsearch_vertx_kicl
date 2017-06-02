package com.lxbluem.stats;

import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.IntStream;

import static org.apache.commons.lang3.math.NumberUtils.min;

@Getter
@Setter
public class MovingAverage {
    private CircularList<Progress> bins;
    private AtomicInteger index;
    private int numberBins;

    public MovingAverage(int numberBins) {
        this.numberBins = numberBins;
        bins = new CircularList<>();
        index = new AtomicInteger(0);
    }

    public double average() {
        Integer idx = index.get();

        return IntStream.range(1, min(bins.size(), numberBins) + 1)
                .mapToDouble(i -> getSpeed(idx, i))
                .average()
                .orElse(0);
    }

    private double getSpeed(Integer idx, int i) {
        Progress p0 = bins.get(idx + i - 1);
        Progress p1 = bins.get(idx + i);
        long dTime = p1.time - p0.time;
        long dSize = p1.size - p0.size;
        double l = (dTime == 0) ? p0.size : 1d * dSize / dTime;
        return l;
    }

    public void addValue(Progress val) {
        Integer idx = index.getAndIncrement();

        if (bins.size() < numberBins) {
            bins.add(val);
            return;
        }

        bins.get(idx).setProgress(val);
    }

    class CircularList<E> extends ArrayList<E> {
        @Override
        public E get(int index) {
            return super.get(index % size());
        }
    }
}
