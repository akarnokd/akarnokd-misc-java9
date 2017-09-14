package hu.akarnokd.java9.flow;

import hu.akarnokd.java9.flow.functionals.FlowFunction2;
import hu.akarnokd.java9.flow.subscribers.FlowReduceSubscriber;

import java.util.Objects;
import java.util.concurrent.Callable;
import java.util.concurrent.Executor;
import java.util.concurrent.Flow;

public final class FlowReduce<T, R> implements FlowAPI<R> {

    final Flow.Publisher<? extends T> source;

    final Callable<? extends R> accumulatorSupplier;

    final FlowFunction2<R, ? super T, R> reducer;

    final Executor executor;

    final int bufferSize;

    public FlowReduce(Flow.Publisher<? extends T> source,
                      Callable<? extends R> accumulatorSupplier,
                      FlowFunction2<R, ? super T, R> reducer, Executor executor, int bufferSize) {
        this.source = source;
        this.accumulatorSupplier = accumulatorSupplier;
        this.reducer = reducer;
        this.executor = executor;
        this.bufferSize = bufferSize;
    }


    @Override
    public void subscribe(Flow.Subscriber<? super R> subscriber) {
        source.subscribe(new ReduceSubscriber<>(subscriber, bufferSize, executor, accumulatorSupplier, reducer));
    }

    static final class ReduceSubscriber<T, R> extends FlowReduceSubscriber<T, R> {
        final Callable<? extends R> accumulatorSupplier;

        final FlowFunction2<R, ? super T, R> reducer;

        R acc;

        public ReduceSubscriber(Flow.Subscriber<? super R> actual, int bufferSize,
                                Executor executor, Callable<? extends R> accumulatorSupplier,
                                FlowFunction2<R, ? super T, R> reducer) {
            super(actual, bufferSize, executor);
            this.accumulatorSupplier = accumulatorSupplier;
            this.reducer = reducer;
        }

        @Override
        protected void onStart() throws Exception {
            acc = Objects.requireNonNull(accumulatorSupplier.call(), "The accumulatorSupplier returned a null value");
        }

        @Override
        protected void onItem(T item, long index) throws Exception {
            acc = Objects.requireNonNull(reducer.apply(acc, item), "The reducer returned a null value");
        }

        @Override
        protected void onEnd(Flow.Subscriber<? super R> actual, long count) {
            actual.onNext(acc);
            if (!isCancelled()) {
                actual.onComplete();
            }
        }
    }
}

