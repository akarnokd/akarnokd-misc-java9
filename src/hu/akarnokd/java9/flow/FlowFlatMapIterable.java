package hu.akarnokd.java9.flow;

import hu.akarnokd.java9.flow.functionals.FlowFunction;

import java.util.concurrent.Executor;
import java.util.concurrent.Flow;

public final class FlowFlatMapIterable<T, R> implements FlowAPI<R> {

    final Flow.Publisher<T> source;

    final FlowFunction<? super T, ? extends Iterable<? extends R>> mapper;

    final Executor executor;

    final int bufferSize;

    public FlowFlatMapIterable(Flow.Publisher<T> source, FlowFunction<? super T, ? extends Iterable<? extends R>> mapper, Executor executor, int bufferSize) {
        this.source = source;
        this.mapper = mapper;
        this.executor = executor;
        this.bufferSize = bufferSize;
    }


    @Override
    public void subscribe(Flow.Subscriber<? super R> subscriber) {
        // TODO implement
    }
}
