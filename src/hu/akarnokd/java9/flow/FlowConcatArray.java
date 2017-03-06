package hu.akarnokd.java9.flow;

import java.util.concurrent.Executor;
import java.util.concurrent.Flow;

public final class FlowConcatArray<T> implements FlowAPI<T> {

    final Flow.Publisher<? extends T>[] sources;

    final Executor executor;


    public FlowConcatArray(Flow.Publisher<? extends T>[] sources, Executor executor) {
        this.sources = sources;
        this.executor = executor;
    }


    @Override
    public void subscribe(Flow.Subscriber<? super T> subscriber) {

    }
}
