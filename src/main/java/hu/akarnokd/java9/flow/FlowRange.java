package hu.akarnokd.java9.flow;

import hu.akarnokd.java9.flow.subscribers.FlowGeneratorSubscription;

import java.util.concurrent.Executor;
import java.util.concurrent.Flow;
import java.util.concurrent.atomic.AtomicLong;

public final class FlowRange implements FlowAPI<Integer> {

    final int start;
    final int end;
    final Executor executor;

    public FlowRange(int start, int count, Executor executor) {
        this.start = start;
        this.end = start + count;
        this.executor = executor;
    }


    @Override
    public void subscribe(Flow.Subscriber<? super Integer> subscriber) {
        RangeSubscription sub = new RangeSubscription(subscriber, start, end, executor);
        sub.request(1);
    }

    static final class RangeSubscription extends FlowGeneratorSubscription<Integer> {
        final int end;

        int index;

        RangeSubscription(Flow.Subscriber<? super Integer> actual, int start, int end, Executor executor) {
            super(actual, executor);
            this.index = start;
            this.end = end;
        }

        @Override
        public void run() {
            Flow.Subscriber<? super Integer> a = actual;

            if (!hasSubscribed) {
                hasSubscribed = true;
                a.onSubscribe(this);
                if (decrementAndGet() == 0) {
                    return;
                }
            }

            long r = get();
            int idx = index;
            int f = end;
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

                    a.onNext(idx);

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
