package hu.akarnokd.java9.flow.tests;

import hu.akarnokd.java9.flow.FlowAPI;
import hu.akarnokd.java9.flow.FlowAPIPlugins;
import hu.akarnokd.java9.flow.FlowRange;
import hu.akarnokd.java9.flow.subscribers.TestFlowSubscriber;
import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class FlowMixedTest {
    @Test
    public void rangeMapFilterTakeSkip() {
        FlowAPIPlugins.executor = Runnable::run;

        FlowAPI.range(1, 10)
                .skip(2)
                .take(5)
                .map(v -> v + 1)
                .filter(v -> v % 2 == 0)
                .test()
                .assertResult(4, 6, 8);
    }

    @Test
    public void flatMapIterable() {
        FlowAPIPlugins.executor = Runnable::run;

        FlowAPI.fromArray(Collections.<Integer>emptyList(), Arrays.asList(1, 2), Collections.<Integer>emptyList(), Arrays.asList(3))
        .flatMapIterable(f -> f)
        .test()
        .assertResult(1, 2, 3)
        ;
    }

    @Test
    public void concatArray() {
        FlowAPIPlugins.executor = Runnable::run;

        FlowAPI.concatArray(FlowAPI.just(1), FlowAPI.range(2, 3), FlowAPI.just(5))
                .test()
                .assertResult(1, 2, 3, 4, 5);
    }

    @Test
    public void collect() {
        FlowAPIPlugins.executor = Runnable::run;

        FlowAPI.range(1, 5)
                .toList()
                .test()
                .assertResult(Arrays.asList(1, 2, 3, 4, 5));
    }

    @Test
    public void sum() {
        FlowAPIPlugins.executor = Runnable::run;

        FlowAPI.range(1, 5)
                .sumInt()
                .test()
                .assertResult(15);
    }

    @Test
    public void max() {
        FlowAPIPlugins.executor = Runnable::run;

        FlowAPI.range(1, 5)
                .maxInt()
                .test()
                .assertResult(5);
    }

    @Test
    public void longSequence() {
        FlowAPIPlugins.executor = Runnable::run;

        FlowAPI.range(1, 1000)
                .map(v -> v)
                .filter(v -> true)
                .take(Long.MAX_VALUE)
                .skip(0)
                .reduce(() -> 0, (a, b) -> b)
                .test()
                .assertResult(1000);
    }

    @Test
    public void longSequenceAsync() {
        FlowAPIPlugins.reset();

        for (int i = 0; i < 1000; i++) {
            FlowAPI.range(1, 1000)
                    .map(v -> v)
                    .filter(v -> true)
                    .take(Long.MAX_VALUE)
                    .skip(0)
                    .reduce(() -> 0, (a, b) -> b)
                    .test()
                    .awaitDone(5, TimeUnit.SECONDS)
                    .assertResult(1000);
        }
    }

    @Test
    public void sumLongAsync() {
        for (int i = 0; i < 1000; i++) {
            FlowAPI.range(1, 1000)
                    .map(v -> 1)
                    .sumLong()
                    .test()
                    .awaitDone(5, TimeUnit.SECONDS)
                    .assertResult(1000L);
        }
    }

    @Test
    public void flatMapIterableAsync() {
        for (int i = 0; i < 1000; i++) {
            FlowAPI.range(1, 1000)
                    .flatMapIterable(v -> Collections.singleton(v))
                    .test()
                    .awaitDone(5, TimeUnit.SECONDS)
                    .assertRange(1, 1000);
        }
    }
}
