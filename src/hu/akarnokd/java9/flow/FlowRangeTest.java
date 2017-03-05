package hu.akarnokd.java9.flow;

import org.junit.Test;

import java.util.Arrays;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.*;

public class FlowRangeTest {
    @Test
    public void normal() {
        FlowRange source = new FlowRange(1, 5, Runnable::run);

        TestFlowSubscriber<Integer> ts = new TestFlowSubscriber<>();

        source.subscribe(ts);

        assertEquals(Arrays.asList(1, 2, 3, 4, 5), ts.values());
        assertEquals(1, ts.completions());
        assertTrue(ts.errors().isEmpty());
    }

    @Test
    public void async() throws InterruptedException {
        FlowRange source = new FlowRange(1, 5, ForkJoinPool.commonPool());
        TestFlowSubscriber<Integer> ts = new TestFlowSubscriber<>();

        source.subscribe(ts);

        assertTrue(ts.await(5, TimeUnit.SECONDS));
        assertEquals(Arrays.asList(1, 2, 3, 4, 5), ts.values());
        assertEquals(1, ts.completions());
        assertTrue(ts.errors().isEmpty());
    }

    @Test
    public void badRequest() {
        FlowRange source = new FlowRange(1, 5, Runnable::run);

        TestFlowSubscriber<Integer> ts = new TestFlowSubscriber<>() {
            @Override
            public void onStart() {
                subscription.request(-99);
            }
        };

        source.subscribe(ts);

        assertTrue(ts.values().isEmpty());
        assertEquals(0, ts.completions());
        assertEquals(1, ts.errors().size());
        assertTrue(ts.errors.get(0) instanceof IllegalArgumentException);
        assertTrue(ts.errors.get(0).getMessage().contains("3.9"));
    }
}
