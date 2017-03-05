package hu.akarnokd.java9.flow;

import hu.akarnokd.java9.flow.functionals.FlowPredicate;
import hu.akarnokd.java9.flow.subscribers.FlowAsyncSubscriber;

import java.util.concurrent.Executor;
import java.util.concurrent.Flow;

public final class FlowFilter<T> implements Flow.Publisher<T> {

    final Flow.Publisher<? extends T> source;

    final FlowPredicate<? super T> predicate;

    final Executor executor;

    final int bufferSize;

    public FlowFilter(Flow.Publisher<? extends T> source, FlowPredicate<? super T> predicate, Executor executor, int bufferSize) {
        this.source = source;
        this.predicate = predicate;
        this.executor = executor;
        this.bufferSize = bufferSize;
    }

    @Override
    public void subscribe(Flow.Subscriber<? super T> subscriber) {
        source.subscribe(new FilterSubscriber<>(subscriber, predicate, executor, bufferSize));
    }

    static final class FilterSubscriber<T> extends FlowAsyncSubscriber<T, T> {

        final FlowPredicate<? super T> predicate;

        public FilterSubscriber(Flow.Subscriber<? super T> actual, FlowPredicate<? super T> predicate, Executor executor, int bufferSize) {
            super(actual, executor, bufferSize);
            this.predicate = predicate;
        }

        @Override
        protected OnItemResult onItem(Flow.Subscriber<? super T> a, T item, long index) throws Exception {
            if (predicate.test(item)) {
                a.onNext(item);
                return OnItemResult.CONTINUE;
            }
            return OnItemResult.SKIP;
        }
    }
}
