package hu.akarnokd.java9.flow;

import java.util.concurrent.Executor;
import java.util.concurrent.Flow;

public final class FlowSkip<T> implements Flow.Publisher<T> {

    final Flow.Publisher<? extends T> source;

    final long n;

    final Executor executor;

    final int bufferSize;

    public FlowSkip(Flow.Publisher<? extends T> source, long n, Executor executor, int bufferSize) {
        this.source = source;
        this.n = n;
        this.executor = executor;
        this.bufferSize = bufferSize;
    }

    @Override
    public void subscribe(Flow.Subscriber<? super T> subscriber) {
        source.subscribe(new SkipSubscriber<>(subscriber, n, executor, bufferSize));
    }

    static final class SkipSubscriber<T> extends FlowAsyncSubscriber<T, T> {

        final long n;

        public SkipSubscriber(Flow.Subscriber<? super T> actual, long n, Executor executor, int bufferSize) {
            super(actual, executor, bufferSize);
            this.n = n;
        }

        @Override
        protected OnItemResult onItem(Flow.Subscriber<? super T> a, T item, long index) throws Exception {
            if (index < n) {
                return OnItemResult.SKIP;
            }
            a.onNext(item);
            return OnItemResult.CONTINUE;
        }
    }
}
