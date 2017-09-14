package hu.akarnokd.java9.flow;

import hu.akarnokd.java9.flow.subscribers.FlowGeneratorSubscription;

import java.util.concurrent.Executor;
import java.util.concurrent.Flow;

public final class FlowArray<T> implements FlowAPI<T> {

    final T[] array;

    final Executor executor;

    public FlowArray(T[] array, Executor executor) {
        this.array = array;
        this.executor = executor;
    }


    @Override
    public void subscribe(Flow.Subscriber<? super T> subscriber) {
        ArraySubscription<T> sub = new ArraySubscription<>(subscriber, executor, array);
        sub.request(1);
    }

    static final class ArraySubscription<T> extends FlowGeneratorSubscription<T> {
        final T[] array;

        int index;

        ArraySubscription(Flow.Subscriber<? super T> actual, Executor executor, T[] array) {
            super(actual, executor);
            this.array = array;
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

            T[] q = array;
            int f = q.length;
            long r = get();
            int idx = index;
            long e = 0L;

            for (;;) {

                while (e != r && idx != f) {
                    if (cancelled) {
                        return;
                    }
                    if (badRequest) {
                        cancelled = true;
                        a.onError(new IllegalArgumentException("§3.9 violated: request must be positive"));
                        return;
                    }

                    T v = q[idx];

                    if (v == null) {
                        cancelled = true;
                        a.onError(new NullPointerException("Array element is null"));
                        return;
                    }

                    a.onNext(v);

                    e++;
                    idx++;
                }

                if (idx == f) {
                    if (!cancelled) {
                        a.onComplete();
                    }
                    return;
                };

                r = get();
                if (e == r) {
                    index = idx;
                    r = addAndGet(-r);
                    if (r == 0L) {
                        break;
                    }
                    e = 0L;
                }
            }
        }
    }
}
