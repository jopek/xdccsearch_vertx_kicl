package com.lxbluem;

import com.lxbluem.state.MovingAverage;
import com.lxbluem.state.Progress;
import org.junit.Before;
import org.junit.Test;

import static java.time.Instant.now;
import static org.junit.Assert.assertEquals;


public class MovingAverageTest {

    private MovingAverage movingAverage;

    @Before
    public void setUp() {
        movingAverage = new MovingAverage(1);
    }

    @Test
    public void averages_are_correct_for_one_element() {
        movingAverage.addValue(new Progress(20, now().toEpochMilli()));
        assertEquals(20d, movingAverage.average(), 0.01);
    }

    @Test
    public void averages_are_correct_for_n_elements() {
        movingAverage.addValue(new Progress(20, 0));
        movingAverage.addValue(new Progress(40, 100));
        assertEquals(0.2d, movingAverage.average(), 0.01);

        movingAverage.addValue(new Progress(60, 200));
        assertEquals(0.2d, movingAverage.average(), 0.01);
    }

    @Test
    public void averages_are_correct_for_N_elements() {
        movingAverage.addValue(new Progress(20, 0));
        movingAverage.addValue(new Progress(40, 100));
        movingAverage.addValue(new Progress(60, 200));
        movingAverage.addValue(new Progress(100, 400));
        assertEquals(0.2d, movingAverage.average(), 0.01);
    }

    @Test
    public void averages_are_correct_for_4_elements() {
        long milli = now().toEpochMilli();
        movingAverage.addValue(new Progress(0, milli));
        movingAverage.addValue(new Progress(40, milli + 100));
        movingAverage.addValue(new Progress(60, milli + 200));
        assertEquals(0.3d, movingAverage.average(), 0.01);

        movingAverage.addValue(new Progress(10000, milli + 10000));
        assertEquals(1.0d, movingAverage.average(), 0.01);
    }


}