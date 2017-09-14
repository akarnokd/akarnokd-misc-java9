package hu.akarnokd.java9.flow;

import hu.akarnokd.java9.flow.subscribers.FlowGeneratorSubscription;

import java.util.Iterator;
import java.util.Objects;
import java.util.concurrent.Executor;
import java.util.concurrent.Flow;

public final class FlowIterable<T> implements FlowAPI<T> {

    final Iterable<? extends T> source;

    final Executor executor;

    public FlowIterable(Iterable<? extends T> source, Executor executor) {
        this.source = source;
        this.executor = executor;
    }


    @Override
    public void subscribe(Flow.Subscriber<? super T> subscriber) {
        IteratorSubscription<T> sub = new IteratorSubscription<>(subscriber, executor, source);
        sub.request(1);
    }

    static final class IteratorSubscription<T> extends FlowGeneratorSubscription<T> {

        final Iterable<? extends T> source;

        Iterator<? extends T> iterator;

        IteratorSubscription(Flow.Subscriber<? super T> actual, Executor executor, Iterable<? extends T> source) {
            super(actual, executor);
            this.source = source;
        }

        @Override
        public void run() {
            Flow.Subscriber<? super T> a = actual;

            Iterator<? extends T> it = iterator;

            if (!hasSubscribed) {
                hasSubscribed = true;
                a.onSubscribe(this);

                boolean b;
                try {
                    it = source.iterator();
                    b = it.hasNext();
                } catch (Throwable ex) {
                    a.onError(ex);
                    return;
                }

                if (!b) {
                    a.onComplete();
                    return;
                }

                iterator = it;
                if (decrementAndGet() == 0) {
                    return;
                }
            }

            Iterator<? extends T> q = it;
            long r = get();
            long e = 0L;

            for (;;) {

                while (e != r) {
                    if (cancelled) {
                        return;
                    }
                    if (badRequest) {
                        cancelled = true;
                        a.onError(new IllegalArgumentException("§3.9 violated: request must be positive"));
                        return;
                    }

                    T v;
                    try {
                        v = Objects.requireNonNull(it.next(), "The iterator returned a null item");
                    } catch (Throwable ex) {
                        a.onError(ex);
                        return;
                    }

                    a.onNext(v);

                    if (cancelled) {
                        return;
                    }

                    boolean b;

                    try {
                        b = it.hasNext();
                    } catch (Throwable ex) {
                        a.onError(ex);
                        return;
                    }

                    if (cancelled) {
                        return;
                    }

                    if (!b) {
                        a.onComplete();
                        return;
                    }

                    e++;
                }

                r = get();
                if (e == r) {
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
