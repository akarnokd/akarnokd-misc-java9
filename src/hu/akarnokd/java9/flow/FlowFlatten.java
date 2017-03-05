package hu.akarnokd.java9.flow;

import java.util.concurrent.Executor;
import java.util.concurrent.Flow;

public final class FlowFlatten<T> implements FlowAPI<T> {

    final Flow.Publisher<? extends Flow.Publisher<? extends T>> sources;

    final Executor executor;

    final int bufferSize;


    public FlowFlatten(Flow.Publisher<? extends Flow.Publisher<? extends T>> sources, Executor executor, int bufferSize) {
        this.sources = sources;
        this.executor = executor;
        this.bufferSize = bufferSize;
    }

    @Override
    public void subscribe(Flow.Subscriber<? super T> subscriber) {
        // TODO implement
    }
}
