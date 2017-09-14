package hu.akarnokd.java9.flow;

import hu.akarnokd.java9.flow.functionals.FlowConsumer2;
import hu.akarnokd.java9.flow.subscribers.FlowReduceSubscriber;

import java.util.Objects;
import java.util.concurrent.Callable;
import java.util.concurrent.Executor;
import java.util.concurrent.Flow;

public final class FlowCollect<T, C> implements FlowAPI<C> {

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
        source.subscribe(new CollectSubscriber<>(subscriber, bufferSize, executor, collectionSupplier, collector));
    }

    static final class CollectSubscriber<T, C> extends FlowReduceSubscriber<T, C> {

        final Callable<? extends C> collectionSupplier;

        final FlowConsumer2<? super C, ? super T> collector;

        C collection;

        public CollectSubscriber(Flow.Subscriber<? super C> actual, int bufferSize, Executor executor, Callable<? extends C> collectionSupplier, FlowConsumer2<? super C, ? super T> collector) {
            super(actual, bufferSize, executor);
            this.collectionSupplier = collectionSupplier;
            this.collector = collector;
        }


        @Override
        protected void onStart() throws Exception {
            collection = Objects.requireNonNull(collectionSupplier.call(), "The collectionSupplier returned a null value");
        }

        @Override
        protected void onItem(T item, long index) throws Exception {
            collector.accept(collection, item);
        }

        @Override
        protected void onEnd(Flow.Subscriber<? super C> actual, long count) {
            actual.onNext(collection);
            if (!isCancelled()) {
                actual.onComplete();
            }
        }
    }
}
