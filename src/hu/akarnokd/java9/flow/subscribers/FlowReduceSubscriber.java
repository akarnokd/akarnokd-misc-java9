package hu.akarnokd.java9.flow.subscribers;

import java.lang.invoke.MethodHandles;
import java.lang.invoke.VarHandle;
import java.util.Arrays;
import java.util.concurrent.Executor;
import java.util.concurrent.Flow;
import java.util.concurrent.atomic.AtomicInteger;

public abstract class FlowReduceSubscriber<T, R>
extends AtomicInteger
implements Flow.Subscriber<T>, Flow.Subscription, Runnable {

    final Flow.Subscriber<? super R> actual;

    final int limit;

    final Executor executor;

    final T[] queue;

    static final VarHandle QUEUE;

    Flow.Subscription subscription;

    volatile boolean requested;
    volatile boolean cancelled;
    volatile boolean done;
    volatile boolean badRequest;
    Throwable error;

    boolean hasSubscribed;
    long producerIndex;
    long consumerIndex;
    int consumed;

    static {
        try {
            QUEUE = MethodHandles.arrayElementVarHandle(Object[].class);
        } catch (Exception ex) {
            throw new InternalError(ex);
        }
    }

    public FlowReduceSubscriber(Flow.Subscriber<? super R> actual, int bufferSize, Executor executor) {
        this.actual = actual;
        this.limit = bufferSize - (bufferSize >> 2);
        this.executor = executor;
        this.queue = (T[])new Object[bufferSize];
    }

    @Override
    public final void onSubscribe(Flow.Subscription subscription) {
        this.subscription = subscription;
        schedule();
    }

    @Override
    public final void onNext(T item) {
        T[] a = queue;
        int m = a.length - 1;
        long pi = producerIndex;
        int offset = (int)pi & m;
        QUEUE.setRelease(a, offset, item);
        producerIndex = pi + 1;
        schedule();
    }

    @Override
    public final void onError(Throwable throwable) {
        error = throwable;
        done = true;
        schedule();
    }

    @Override
    public final void onComplete() {
        done = true;
        schedule();
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

    protected final boolean isCancelled() {
        return cancelled;
    }

    protected final void schedule() {
        if (getAndIncrement() == 0) {
            executor.execute(this);
        }
    }

    @Override
    public final void run() {
        Flow.Subscriber<? super R> a = actual;

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
        }

        T[] q = queue;
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
                        cancelled = true;
                        onEnd(a, ci);
                    }
                    break;
                }

                if (empty) {
                    break;
                }

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

        // TODO implement
    }

    protected abstract void onStart() throws Exception;

    protected abstract void onItem(T item, long index) throws Exception;

    protected abstract void onEnd(Flow.Subscriber<? super R> actual, long count);
}
