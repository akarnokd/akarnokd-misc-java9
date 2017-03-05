package hu.akarnokd.java9.flow;

import hu.akarnokd.java9.flow.functionals.FlowConsumer2;

import java.util.concurrent.Callable;
import java.util.concurrent.Executor;
import java.util.concurrent.Flow;

public final class FlowCollect<T, C> implements Flow.Publisher<C> {

    final Flow.Publisher<? extends T> source;

    final Callable<? extends C> collectionSupplier;

    final FlowConsumer2<? super C, ? super T> collector;

    final Executor executor;

    final int bufferSize;

    public FlowCollect(Flow.Publisher<? extends T> source, Callable<? extends C> collectionSupplier, FlowConsumer2<? super C, ? super T> collector, Executor executor, int bufferSize) {
        this.source = source;
        this.collectionSupplier = collectionSupplier;
        this.collector = collector;
        this.executor = executor;
        this.bufferSize = bufferSize;
    }


    @Override
    public void subscribe(Flow.Subscriber<? super C> subscriber) {
        // TODO implement
    }
}
