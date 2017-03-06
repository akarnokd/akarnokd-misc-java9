package hu.akarnokd.java9.flow;

import hu.akarnokd.java9.flow.utils.SubscriptionHelper;

import java.lang.invoke.MethodHandles;
import java.lang.invoke.VarHandle;
import java.util.Arrays;
import java.util.concurrent.Executor;
import java.util.concurrent.Flow;
import java.util.concurrent.atomic.AtomicInteger;

public final class FlowConcatArray<T> implements FlowAPI<T> {

    final Flow.Publisher<? extends T>[] sources;

    final Executor executor;

    final int bufferSize;

    public FlowConcatArray(Flow.Publisher<? extends T>[] sources, Executor executor, int bufferSize) {
        this.sources = sources;
        this.executor = executor;
        this.bufferSize = bufferSize;
    }


    @Override
    public void subscribe(Flow.Subscriber<? super T> subscriber) {
        ConcatArraySubscriber<T> sub = new ConcatArraySubscriber<T>(subscriber, executor, sources, bufferSize);
        sub.schedule();
    }

    static final class ConcatArraySubscriber<T> extends AtomicInteger
    implements Flow.Subscriber<T>, Flow.Subscription, Runnable {

        final Flow.Subscriber<? super T> actual;

        final Executor executor;

        final Flow.Publisher<? extends T>[] sources;

        final T[] queue;
        static final VarHandle QUEUE;

        final int limit;

        volatile Flow.Subscription subscription;
        static final VarHandle SUBSCRIPTION;

        volatile boolean cancelled;
        volatile boolean badRequest;
        volatile boolean done;
        Throwable error;

        volatile long requested;
        static final VarHandle REQUESTED;

        long producerIndex;

        long consumerIndex;
        int index;
        int consumed;

        static {
            try {
                QUEUE = MethodHandles.arrayElementVarHandle(Object[].class);
                SUBSCRIPTION = MethodHandles.lookup().findVarHandle(ConcatArraySubscriber.class, "subscription", Flow.Subscription.class);
                REQUESTED = MethodHandles.lookup().findVarHandle(ConcatArraySubscriber.class, "requested", Long.TYPE);
            } catch (Throwable ex) {
                throw new InternalError(ex);
            }
        }

        ConcatArraySubscriber(Flow.Subscriber<? super T> actual, Executor executor, Flow.Publisher<? extends T>[] sources, int bufferSize) {
            this.actual = actual;
            this.executor = executor;
            this.sources = sources;
            this.limit = bufferSize - (bufferSize >> 2);
            this.queue = (T[])new Object[bufferSize];
            this.index = -1;
            this.done = true;
        }

        @Override
        public void onSubscribe(Flow.Subscription subscription) {
            if (SubscriptionHelper.replace(SUBSCRIPTION, this, subscription)) {
                subscription.request(queue.length);
            }
        }

        @Override
        public void onNext(T item) {
            T[] q = queue;
            int m = q.length - 1;
            long pi = producerIndex;
            int offset = (int)pi & m;
            QUEUE.setRelease(q, offset, item);
            producerIndex = pi + 1;
            schedule();
        }

        @Override
        public void onError(Throwable throwable) {
            error = throwable;
            done = true;
            schedule();
        }

        @Override
        public void onComplete() {
            done = true;
            schedule();
        }

        @Override
        public void request(long n) {
            if (n <= 0) {
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
        public void cancel() {
            cancelled = true;
            SubscriptionHelper.cancel(SUBSCRIPTION, this);
            schedule();
        }

        void schedule() {
            if (getAndIncrement() == 0) {
                executor.execute(this);
            }
        }

        @Override
        public void run() {
            Flow.Subscriber<? super T> a = actual;
            int idx = index;
            if (idx == -1) {
                idx = 0;
                a.onSubscribe(this);
            }

            long ci = consumerIndex;
            int missed = 1;
            T[] q = queue;
            int m = q.length - 1;
            int f = consumed;
            int lim = limit;

            for (;;) {

                long r = requested;

                while (ci != r) {
                    if (cancelled) {
                        Arrays.fill(q, null);
                        break;
                    }
                    if (badRequest) {
                        cancelled = true;
                        SubscriptionHelper.cancel(SUBSCRIPTION, this);
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
                            cancelled = true;
                            a.onError(ex);
                        } else
                        if (idx == sources.length) {
                            cancelled = true;
                            a.onComplete();
                            break;
                        } else {
                            done = false;
                            Flow.Publisher<? extends T> p = sources[idx++];

                            if (p == null) {
                                cancelled = true;
                                a.onError(new NullPointerException("One of the sources is null"));
                            } else {
                                f = 0;
                                p.subscribe(this);
                            }
                        }
                        break;
                    }

                    if (empty) {
                        break;
                    }

                    QUEUE.setRelease(q, offset, null);

                    a.onNext(v);

                    ci++;
                    if (++f == lim) {
                        f = 0;
                        subscription.request(lim);
                    }
                }

                if (ci == r) {
                    boolean d = done;
                    boolean empty = null == QUEUE.getAcquire(q, (int)ci & m);
                    if (d && empty) {
                        Throwable ex = error;
                        if (ex != null) {
                            cancelled = true;
                            a.onError(ex);
                        } else
                        if (idx == sources.length) {
                            cancelled = true;
                            a.onComplete();
                            break;
                        } else {
                            done = false;
                            Flow.Publisher<? extends T> p = sources[idx++];

                            if (p == null) {
                                cancelled = true;
                                a.onError(new NullPointerException("One of the sources is null"));
                            } else {
                                f = 0;
                                p.subscribe(this);
                            }
                        }
                    }
                }

                int w = get();
                if (w == missed) {
                    consumerIndex = ci;
                    index = idx;
                    consumed = f;
                    missed = addAndGet(-missed);
                    if (missed == 0) {
                        break;
                    }
                } else {
                    missed = w;
                }
            }
        }
    }
}
