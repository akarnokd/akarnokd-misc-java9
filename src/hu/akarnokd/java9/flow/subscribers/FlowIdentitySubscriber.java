package hu.akarnokd.java9.flow.subscribers;

import java.lang.invoke.MethodHandles;
import java.lang.invoke.VarHandle;
import java.util.Arrays;
import java.util.concurrent.Executor;
import java.util.concurrent.Flow;
import java.util.concurrent.atomic.AtomicInteger;

public abstract class FlowIdentitySubscriber<T, R> extends FlowAsyncSubscriber<T, R> {

    volatile long requested;
    static final VarHandle REQUESTED;

    static {
        try {
            REQUESTED = MethodHandles.lookup().findVarHandle(FlowIdentitySubscriber.class, "requested", Long.TYPE);
        } catch (Exception ex) {
            throw new InternalError(ex);
        }
    }

    public FlowIdentitySubscriber(Flow.Subscriber<? super R> actual, Executor executor, int bufferSize) {
        super(actual, executor, bufferSize);
    }

    @Override
    public final void request(long n) {
        if (n <= 0L) {
            badRequest = true;
        } else {
            for (;;) {
                long r = requested;
                long u = r + n;
                if (u < 0L) {
                    u = Long.MAX_VALUE;
                }
                if (REQUESTED.compareAndSet(this, r, u)) {
                    break;
                }
            }
        }
        schedule();
    }

    @Override
    public final void run() {
        Flow.Subscriber<? super R> a = actual;
        T[] q = queue;

        if (!hasSubscribed) {
            hasSubscribed = true;
            a.onSubscribe(this);
            subscription.request(q.length);
        }

        int m = q.length - 1;
        int missed = 1;
        long e = emitted;
        int f = consumed;
        int lim = limit;
        long ci = consumerIndex;

        for (;;) {

            if (cancelled) {
                Arrays.fill(q, null);
            } else {
                long r = requested;

                while (e != r) {
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
                        cancelled = true;
                        Throwable ex = error;
                        if (ex != null) {
                            a.onError(ex);
                        } else {
                            a.onComplete();
                        }
                        break;
                    }

                    if (empty) {
                        break;
                    }

                    OnItemResult result;
                    try {
                        result = onItem(a, v, ci);
                    } catch (Throwable ex) {
                        cancelled = true;
                        subscription.cancel();
                        Arrays.fill(q, null);
                        a.onError(ex);
                        break;
                    }

                    if (result == OnItemResult.STOP) {
                        cancelled = true;
                        subscription.cancel();
                        Arrays.fill(q, null);
                        a.onComplete();
                        break;
                    }
                    if (result == OnItemResult.CONTINUE) {
                        e++;
                    }
                    ci++;

                    if (++f == lim) {
                        f = 0;
                        subscription.request(lim);
                    }
                }

                if (e == r) {
                    if (cancelled) {
                        Arrays.fill(q, null);
                    } else {
                        if (done) {
                            int offset = (int)e & m;
                            boolean empty = null == QUEUE.getAcquire(q, offset);
                            if (empty) {
                                cancelled = true;
                                Throwable ex = error;
                                if (ex != null) {
                                    a.onError(ex);
                                } else {
                                    a.onComplete();
                                }
                            }
                        }
                    }
                }
            }
            int w = get();
            if (w == missed) {
                emitted = e;
                consumed = f;
                consumerIndex = ci;
                missed = addAndGet(-missed);
                if (missed == 0) {
                    break;
                }
            } else {
                missed = w;
            }
        }
    }

    public enum OnItemResult {
        CONTINUE,
        SKIP,
        STOP
    }

    protected abstract OnItemResult onItem(Flow.Subscriber<? super R> a, T item, long index) throws Exception;
}
