package hu.akarnokd.java9.flow;

import hu.akarnokd.java9.flow.subscribers.FlowReduceSubscriber;

import java.util.concurrent.Executor;
import java.util.concurrent.Flow;

public final class FlowSumInt implements Flow.Publisher<Integer> {

    final Flow.Publisher<? extends Number> source;

    final Executor executor;

    final int bufferSize;

    public FlowSumInt(Flow.Publisher<? extends Number> source, Executor executor, int bufferSize) {
        this.source = source;
        this.executor = executor;
        this.bufferSize = bufferSize;
    }


    @Override
    public void subscribe(Flow.Subscriber<? super Integer> subscriber) {
        source.subscribe(new SumIntSubscriber(subscriber, bufferSize, executor));
    }

    static final class SumIntSubscriber extends FlowReduceSubscriber<Number, Integer> {

        int sum;

        public SumIntSubscriber(Flow.Subscriber<? super Integer> actual, int bufferSize, Executor executor) {
            super(actual, bufferSize, executor);
        }

        @Override
        protected void onStart() throws Exception {
        }

        @Override
        protected void onItem(Number item, long index) throws Exception {
            sum += item.intValue();
        }

        @Override
        protected void onEnd(Flow.Subscriber<? super Integer> actual, long count) {
            if (count != 0L) {
                actual.onNext(sum);
            }
            if (!isCancelled()) {
                actual.onComplete();
            }
        }
    }
}

