package hu.akarnokd.java9.flow.subscribers;

import java.util.concurrent.Executor;
import java.util.concurrent.Flow;
import java.util.concurrent.atomic.AtomicLong;

public abstract class FlowGeneratorSubscription<T>
extends AtomicLong
implements Flow.Subscription, Runnable {
    protected final Flow.Subscriber<? super T> actual;
    protected final Executor executor;

    protected boolean hasSubscribed;

    protected volatile boolean cancelled;
    protected volatile boolean badRequest;

    public FlowGeneratorSubscription(Flow.Subscriber<? super T> actual, Executor executor) {
        this.actual = actual;
        this.executor = executor;
    }

    @Override
    public final void request(long n) {
        if (n <= 0L) {
            badRequest = true;
            n = 1;
        }
        for (;;) {
            long r = get();
            long u = r + n;
            if (u < 0L) {
                u = Long.MAX_VALUE;
            }
            if (compareAndSet(r, u)) {
                if (r == 0L) {
                    executor.execute(this);
                }
                break;
            }
        }
    }

    @Override
    public void cancel() {
        cancelled = true;
    }
}
