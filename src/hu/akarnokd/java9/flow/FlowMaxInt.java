package hu.akarnokd.java9.flow;

import hu.akarnokd.java9.flow.subscribers.FlowReduceSubscriber;

import java.util.concurrent.Executor;
import java.util.concurrent.Flow;

public final class FlowMaxInt implements Flow.Publisher<Integer> {

    final Flow.Publisher<? extends Number> source;

    final Executor executor;

    final int bufferSize;

    public FlowMaxInt(Flow.Publisher<? extends Number> source, Executor executor, int bufferSize) {
        this.source = source;
        this.executor = executor;
        this.bufferSize = bufferSize;
    }


    @Override
    public void subscribe(Flow.Subscriber<? super Integer> subscriber) {
        source.subscribe(new MaxIntSubscriber(subscriber, bufferSize, executor));
    }

    static final class MaxIntSubscriber extends FlowReduceSubscriber<Number, Integer> {

        int max;

        public MaxIntSubscriber(Flow.Subscriber<? super Integer> actual, int bufferSize, Executor executor) {
            super(actual, bufferSize, executor);
        }

        @Override
        protected void onStart() throws Exception {
        }

        @Override
        protected void onItem(Number item, long index) throws Exception {
            if (index == 0) {
                max = item.intValue();
            } else {
                max = Math.max(max, item.intValue());
            }
        }

        @Override
        protected void onEnd(Flow.Subscriber<? super Integer> actual, long count) {
            if (count != 0L) {
                actual.onNext(max);
            }
            if (!isCancelled()) {
                actual.onComplete();
            }
        }
    }
}

