package hu.akarnokd.java9.flow;

import hu.akarnokd.java9.flow.subscribers.FlowGeneratorSubscription;

import java.util.concurrent.Executor;
import java.util.concurrent.Flow;

public final class FlowJust<T> implements FlowAPI<T> {

    final T item;

    final Executor executor;

    public FlowJust(T item, Executor executor) {
        this.item = item;
        this.executor = executor;
    }


    @Override
    public void subscribe(Flow.Subscriber<? super T> subscriber) {
        JustSubscription<T> sub = new JustSubscription<T>(subscriber, executor, item);
        sub.request(1);
    }

    static final class JustSubscription<T> extends FlowGeneratorSubscription<T> {

        final T item;

        public JustSubscription(Flow.Subscriber<? super T> actual, Executor executor, T item) {
            super(actual, executor);
            this.item = item;
        }

        @Override
        public void run() {
            Flow.Subscriber<? super T> a = actual;

            if (!hasSubscribed) {
                hasSubscribed = true;
                a.onSubscribe(this);
                if (decrementAndGet() == 0) {
                    return;
                }
            }

            if (cancelled) {
                return;
            }

            if (badRequest) {
                cancelled = true;
                a.onError(new IllegalArgumentException("§3.9 violated: request must be positive"));
                return;
            }

            a.onNext(item);
            if (!cancelled) {
                a.onComplete();
            }
        }
    }
}
