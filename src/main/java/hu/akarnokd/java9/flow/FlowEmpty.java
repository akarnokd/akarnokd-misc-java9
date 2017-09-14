package hu.akarnokd.java9.flow;

import java.util.concurrent.Executor;
import java.util.concurrent.Flow;

public final class FlowEmpty<T> implements FlowAPI<T> {

    final Executor executor;


    public FlowEmpty(Executor executor) {
        this.executor = executor;
    }

    @Override
    public void subscribe(Flow.Subscriber<? super T> subscriber) {
        executor.execute(new EmptySubscription(subscriber));
    }

    static final class EmptySubscription implements Flow.Subscription, Runnable {

        final Flow.Subscriber<?> actual;

        volatile boolean cancelled;
        volatile boolean badRequest;

        EmptySubscription(Flow.Subscriber<?> actual) {
            this.actual = actual;
        }

        @Override
        public void run() {
            Flow.Subscriber<?> a = actual;
            a.onSubscribe(this);
            if (!cancelled) {
                if (badRequest) {
                    a.onError(new IllegalArgumentException("§3.9 violated: non-positive request received"));
                } else {
                    a.onComplete();
                }
            }
        }

        @Override
        public void request(long n) {
            if (n <= 0L) {
                badRequest = true;
            }
        }

        @Override
        public void cancel() {
            cancelled = true;
        }
    }
}
