package hu.akarnokd.java9.flow;

import hu.akarnokd.java9.flow.subscribers.FlowIdentitySubscriber;

import java.util.concurrent.Executor;
import java.util.concurrent.Flow;

public final class FlowTake<T> implements FlowAPI<T> {

    final Flow.Publisher<? extends T> source;

    final long n;

    final Executor executor;

    final int bufferSize;

    public FlowTake(Flow.Publisher<? extends T> source, long n, Executor executor, int bufferSize) {
        this.source = source;
        this.n = n;
        this.executor = executor;
        this.bufferSize = bufferSize;
    }

    @Override
    public void subscribe(Flow.Subscriber<? super T> subscriber) {
        source.subscribe(new TakeSubscriber<>(subscriber, n, executor, bufferSize));
    }

    static final class TakeSubscriber<T> extends FlowIdentitySubscriber<T, T> {

        final long n;

        public TakeSubscriber(Flow.Subscriber<? super T> actual, long n, Executor executor, int bufferSize) {
            super(actual, executor, bufferSize);
            this.n = n;
        }

        @Override
        protected OnItemResult onItem(Flow.Subscriber<? super T> a, T item, long index) throws Exception {
            long n = this.n;
            if (index < n) {
                a.onNext(item);
                if (index + 1 != n) {
                    return OnItemResult.CONTINUE;
                }
            }
            return OnItemResult.STOP;
        }
    }
}
