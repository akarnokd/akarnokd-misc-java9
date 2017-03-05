package hu.akarnokd.java9.flow;

import hu.akarnokd.java9.flow.functionals.FlowFunction2;

import java.util.concurrent.Callable;
import java.util.concurrent.Executor;
import java.util.concurrent.Flow;

public final class FlowReduce<T, R> implements Flow.Publisher<R> {

    final Flow.Publisher<? extends T> source;

    final Callable<? extends R> accumulator;

    final FlowFunction2<R, T, R> reducer;

    final Executor executor;


    public FlowReduce(Flow.Publisher<? extends T> source, Callable<? extends R> accumulator, FlowFunction2<R, T, R> reducer, Executor executor) {
        this.source = source;
        this.accumulator = accumulator;
        this.reducer = reducer;
        this.executor = executor;
    }


    @Override
    public void subscribe(Flow.Subscriber<? super R> subscriber) {
        // TODO implement
    }
}

