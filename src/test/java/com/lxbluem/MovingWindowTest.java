package com.lxbluem;

import com.lxbluem.stats.Progress;
import lombok.Getter;
import lombok.Setter;
import org.junit.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import static java.util.stream.Collectors.toList;


public class MovingWindowTest {
    private MovingWindow window;

    @Test
    public void window_has_defined_number_of_bins() throws Exception {
        window = new MovingWindow(5);
    }


    public class CircularList<E> extends ArrayList<E> {
        @Override
        public E get(int index) {
            return super.get(index % size());
        }
    }

    @Getter
    @Setter
    class MovingWindow<T> {
        Map<T, CircularList<Progress>> bins;
        Map<T, AtomicInteger> index;
        private int numberBins;

        MovingWindow(int numberBins) {
            this.numberBins = numberBins;
            bins = new HashMap<>();
            index = new HashMap<>();
        }

        public double average(T key) {
            CircularList<Progress> progresses = bins.get(key);
            Integer idx = index.get(key).getAndIncrement();

            Long earliest = bins.get(key)
                    .stream()
                    .filter(p -> p.time != 0)
                    .map(p -> p.time)
                    .min(Long::compareTo)
                    .orElse(1L);

            List<Double> collect = bins.get(key)
                    .stream()
                    .map(progress -> (double) progress.size / progress.time)
                    .collect(toList());
            return 0;
        }

        public void addValue(T key, Progress val) {
            CircularList<Progress> progresses = bins.get(key);
            Integer idx = index.get(key).getAndIncrement();

            Progress next = progresses.get(idx);
            next.size = val.size;
            next.time = val.time;
        }

    }

}