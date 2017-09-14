package hu.akarnokd.java9.flow.subscribers;

import java.lang.invoke.MethodHandles;
import java.lang.invoke.VarHandle;
import java.util.Arrays;
import java.util.concurrent.Executor;
import java.util.concurrent.Flow;
import java.util.concurrent.atomic.AtomicInteger;

public abstract class FlowReduceSubscriber<T, R> extends FlowAsyncSubscriber<T, R> {

    volatile boolean requested;

    int consumed;

    public FlowReduceSubscriber(Flow.Subscriber<? super R> actual, int bufferSize, Executor executor) {
        super(actual, executor, bufferSize);
    }

    @Override
    public final void request(long n) {
        if (n <= 0L) {
            badRequest = true;
        } else {
            requested = true;
        }
        schedule();
    }

    @Override
    public void cancel() {
        cancelled = true;
        subscription.cancel();
        schedule();
    }

    @Override
    public final void run() {
        Flow.Subscriber<? super R> a = actual;
        T[] q = queue;

        if (!hasSubscribed) {
            hasSubscribed = true;
            a.onSubscribe(this);
            try {
                onStart();
            } catch (Throwable ex) {
                cancelled = true;
                subscription.cancel();
                a.onError(ex);
            }
            subscription.request(q.length);
        }

        int m = q.length - 1;
        int missing = 1;
        long ci = consumerIndex;
        int f = consumed;
        int lim = limit;

        for (;;) {

            for (;;) {
                if (cancelled) {
                    Arrays.fill(q, null);
                    break;
                }

                if (badRequest) {
                    cancelled = true;
                    subscription.cancel();
                    Arrays.fill(q, null);
                    a.onError(new IllegalArgumentException("§3.9 violated: non-positive request received"));
                    break;
                }
                boolean d = done;

                int offset = (int)ci & m;
                T v = (T)QUEUE.getAcquire(q, offset);
                boolean empty = v == null;

                if (d && empty) {
                    Throwable ex = error;
                    if (ex != null) {
                        a.onError(ex);
                    } else
                    if (requested) {
                        onEnd(a, ci);
                        cancelled = true;
                    }
                    break;
                }

                if (empty) {
                    break;
                }

                QUEUE.setRelease(q, offset, null);

                try {
                    onItem(v, ci);
                } catch (Throwable ex) {
                    cancelled = true;
                    subscription.cancel();
                    Arrays.fill(q, null);
                    a.onError(ex);
                    break;
                }
                ci++;
                if (++f == lim) {
                    f = 0;
                    subscription.request(lim);
                }
            }

            int w = get();
            if (missing == w) {
                consumed = f;
                consumerIndex = ci;
                missing = addAndGet(-missing);
                if (missing == 0) {
                    break;
                }
            } else {
                missing = w;
            }
        }
    }

    protected abstract void onStart() throws Exception;

    protected abstract void onItem(T item, long index) throws Exception;

    protected abstract void onEnd(Flow.Subscriber<? super R> actual, long count);
}
